package com.paperpig.maimaidata.crawler;

import static com.paperpig.maimaidata.crawler.CrawlerCaller.finishUpdate;
import static com.paperpig.maimaidata.crawler.CrawlerCaller.onError;
import static com.paperpig.maimaidata.crawler.CrawlerCaller.startAuth;
import static com.paperpig.maimaidata.crawler.CrawlerCaller.writeLog;

import android.util.Log;

import androidx.core.util.Predicate;

import com.paperpig.maimaidata.db.entity.RecordEntity;
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity;
import com.paperpig.maimaidata.model.DifficultyType;
import com.paperpig.maimaidata.model.SongType;
import com.paperpig.maimaidata.repository.RecordRepository;
import com.paperpig.maimaidata.repository.SongWithChartRepository;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.ConnectionSpec;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.TlsVersion;

public class WechatCrawler {
    private static final int MAX_RETRY_COUNT = 4;

    private static final String TAG = "Crawler";

    private static final SimpleCookieJar jar = new SimpleCookieJar();

    private static OkHttpClient client = null;

    private final RecordRepository recordRepository;
    private final SongWithChartRepository songWithChartRepository;

    public WechatCrawler(RecordRepository recordRepository, SongWithChartRepository songWithChartRepository) {
        buildHttpClient(false);
        this.recordRepository = recordRepository;
        this.songWithChartRepository = songWithChartRepository;
    }

    private int batchFetchAndUploadData(Set<DifficultyType> difficulties) {
        List<CompletableFuture<List<RecordEntity>>> tasks = new ArrayList<>();
        List<RecordEntity> allRecords = new ArrayList<>();
        for (DifficultyType difficulty : difficulties) {
            tasks.add(CompletableFuture.supplyAsync(() -> fetchAndUploadData(difficulty, 1)));
        }
        for (CompletableFuture<List<RecordEntity>> task : tasks) {
            allRecords.addAll(task.join());
        }
        Log.d(TAG, "共获取到" + allRecords.size() + "条数据");
        recordRepository.replaceAllRecord(allRecords);
        return allRecords.size();
    }

    private List<RecordEntity> fetchAndUploadData(DifficultyType difficulty, Integer retryCount) {
        writeLog("开始获取 " + difficulty.getDisplayName() + " 难度的数据");
        Log.d(TAG, "开始获取 " + difficulty.getDisplayName() + " 难度的数据");
        Request request = new Request.Builder().url("https://maimai.wahlap.com/maimai-mobile/record/musicGenre/search/?genre=99&diff=" + difficulty.getWebDifficultyIndex()).build();

        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            String data = Objects.requireNonNull(response.body()).string();
            Matcher matcher = Pattern.compile("<html.*>([\\s\\S]*)</html>").matcher(data);
            if (matcher.find()) {
                data = Objects.requireNonNull(matcher.group(1));
            }
            data = Pattern.compile("\\s+").matcher(data).replaceAll(" ");

            writeLog(difficulty.getDisplayName() + " 难度的数据已获取，正在处理");
            Log.d(TAG, difficulty.getDisplayName() + " 难度的数据已获取，正在处理");
            return parsePageToRecordList(data, difficulty);
        } catch (Exception e) {
            return handleRetryFetchAndUploadData(e, difficulty, retryCount);
        }
    }

    private static final List<Integer> linkCofDxScore = List.of(834, 1011, 1275, 1839);

    private static final Pattern icoPattern = Pattern.compile("_icon_(.*)\\.png");
    private static final List<String> fcIcons = List.of("fc", "fcp", "ap", "app");
    private static final List<String> fsIcons = List.of("fs", "fsp", "fdx", "fdxp");

    private List<RecordEntity> parsePageToRecordList(String pageData, DifficultyType difficulty) {
        List<RecordEntity> records = new ArrayList<>();

        for (Element nameElement : Jsoup.parse(pageData).select("div.music_name_block.t_l.f_13.break")) {
            try {
                String title = nameElement.text();
                boolean isUtage = difficulty == DifficultyType.UTAGE;

                if (title.isEmpty()) {
                    title = "　";
                }

                Element parentRow = nameElement.parent();
                if (parentRow == null) {
                    continue;
                }
                Elements siblings = parentRow.children();

                String achievementText = findText(siblings, "music_score_block", s -> s.contains("%"));
                if (achievementText == null) {
                    continue;
                }

                Element superParentRow = nameElement.parent().parent().parent();
                Element typeImg = superParentRow.select("img.music_kind_icon").first();
                SongType type = SongType.DX;
                if (typeImg != null) {
                    if (typeImg.attr("src").contains("standard")) {
                        type = SongType.SD;
                    }
                } else if (isUtage) {
                    type = SongType.UTAGE;
                } else {
                    Log.w(TAG, "无法获取铺面类型，推测为DX铺面");
                }

                String dxScoreText = findText(siblings, "music_score_block", s -> s.contains("/"));
                if (dxScoreText == null) {
                    continue;
                }
                String[] dxScorePart = dxScoreText.replace(",", "").replace(" ", "").split("/");
                int dxScore = Integer.parseInt(dxScorePart[0]);
                int dxMaxScore = Integer.parseInt(dxScorePart[1]);
                if (title.equals("Link")) {
                    if (dxMaxScore == linkCofDxScore.get(difficulty.getDifficultyIndex())) {
                        title = "Link(Cof)";
                    }
                }

                int song_id = -1;
                SongWithChartsEntity song_entity = null;
                List<SongWithChartsEntity> searchSongs = songWithChartRepository.searchSongsWithTitle(title);
                for (SongWithChartsEntity song : searchSongs) {
                    if (song.getSongData().getType().equals(type)) {
                        song_id = song.getSongData().getId();
                        song_entity = song;
                        break;
                    }
                }
                if (song_id == -1) {
                    Log.w(TAG, "无法获取 " + title + "(" + type + ") " + difficulty.getDisplayName() + " 的歌曲数据");
                    writeLog("无法获取 " + title + "(" + type + ") " + difficulty.getDisplayName() + " 的歌曲数据");
                    continue;
                }

                boolean isBuddy = isUtage && Boolean.TRUE.equals(song_entity.getSongData().getBuddy());
                String level = song_entity.getChartsMap().get(difficulty).getLevel();

                String fc = "";
                String fs = "";

                for (Element sibling : siblings) {
                    Element img = sibling.select("img[src*=_icon_]").first();
                    if (img != null) {
                        Matcher m = icoPattern.matcher(img.attr("src"));
                        if (m.find()) {
                            String iconType = m.group(1);
                            if (iconType != null) {
                                if (fcIcons.contains(iconType)) {
                                    fc = iconType;
                                } else if (fsIcons.contains(iconType)) {
                                    if (iconType.equals("fdx")) {
                                        fs = "fsd";
                                    } else if (iconType.equals("fdxp")) {
                                        fs = "fsdp";
                                    } else {
                                        fs = iconType;
                                    }
                                }
                            }
                        }
                    }
                }

                double achievements = Double.parseDouble(achievementText.replace("%", ""));

                String rate;
                double rateAchievements = isBuddy ? achievements / 2 : achievements;
                if (rateAchievements < 50) {
                    rate = "d";
                } else if (rateAchievements < 60) {
                    rate = "c";
                } else if (rateAchievements < 70) {
                    rate = "b";
                } else if (rateAchievements < 75) {
                    rate = "bb";
                } else if (rateAchievements < 80) {
                    rate = "bbb";
                } else if (rateAchievements < 90) {
                    rate = "a";
                } else if (rateAchievements < 94) {
                    rate = "aa";
                } else if (rateAchievements < 97) {
                    rate = "aaa";
                } else if (rateAchievements < 98) {
                    rate = "s";
                } else if (rateAchievements < 99) {
                    rate = "sp";
                } else if (rateAchievements < 99.5) {
                    rate = "ss";
                } else if (rateAchievements < 100) {
                    rate = "ssp";
                } else if (rateAchievements < 100.5) {
                    rate = "sss";
                } else {
                    rate = "sssp";
                }

                records.add(new RecordEntity(
                    0,
                    song_id,
                    achievements,
                    dxScore,
                    fc,
                    fs,
                    level,
                    difficulty,
                    rate
                ));
                if (isBuddy) {
                    records.add(new RecordEntity(
                        0,
                        song_id,
                        achievements,
                        dxScore,
                        fc,
                        fs,
                        level,
                        DifficultyType.UTAGE_PLAYER2,
                        rate
                    ));
                }
            } catch (Exception e) {
                writeLog("解析记录时出错: " + e.getMessage());
                Log.w(TAG, "解析记录时出错: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        return records;
    }

    private static String findText(Elements siblings, String clazz, Predicate<String> predicate) {
        for (Element sibling : siblings) {
            if (sibling.hasClass(clazz) && (predicate == null || predicate.test(sibling.text()))) {
                return sibling.text();
            }
        }
        return null;
    }

    private List<RecordEntity> handleRetryFetchAndUploadData(Exception e, DifficultyType difficulty, Integer currentRetryCount) {
        writeLog("获取 " + difficulty.getDisplayName() + " 难度数据时出现错误: " + e);
        if (currentRetryCount < MAX_RETRY_COUNT) {
            writeLog("进行第" + currentRetryCount + "次重试");
            return fetchAndUploadData(difficulty, currentRetryCount + 1);
        } else {
            writeLog(difficulty.getDisplayName() + "难度数据更新失败！");
        }
        return new ArrayList<>();
    }

    protected static String getWechatAuthUrl() throws IOException {
        buildHttpClient(true);

        Request request = new Request.Builder().addHeader("Host", "tgk-wcaime.wahlap.com").addHeader("Upgrade-Insecure-Requests", "1").addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12; IN2010 Build/RKQ1.211119.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/86.0.4240.99 XWEB/4317 MMWEBSDK/20220903 Mobile Safari/537.36 MMWEBID/363 MicroMessenger/8.0.28.2240(0x28001C57) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64").addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/wxpic,image/tpg,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9").addHeader("X-Requested-With", "com.tencent.mm").addHeader("Sec-Fetch-Site", "none").addHeader("Sec-Fetch-Mode", "navigate").addHeader("Sec-Fetch-User", "?1").addHeader("Sec-Fetch-Dest", "document").addHeader("Accept-Encoding", "gzip, deflate").addHeader("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7").url("https://tgk-wcaime.wahlap.com/wc_auth/oauth/authorize/maimai-dx").build();

        Call call = client.newCall(request);
        Response response = call.execute();
        String url = response.request().url().toString().replace("redirect_uri=https", "redirect_uri=http");

        Log.d(TAG, "Auth url:" + url);
        return url;
    }

    public void startFetch(Set<DifficultyType> difficulties, String wechatAuthUrl) {
        if (wechatAuthUrl.startsWith("http")) {
            wechatAuthUrl = wechatAuthUrl.replaceFirst("http", "https");
        }

        jar.clearCookieStroe();

        // Login wechat
        try {
            startAuth();
            writeLog("开始登录net，请稍后...");
            this.loginWechat(wechatAuthUrl);
            writeLog("登陆完成");
        } catch (Exception error) {
            writeLog("登陆时出现错误:\n");
            onError(error);
            return;
        }

        // Fetch maimai data
        try {
            int dataSize = this.fetchMaimaiData(difficulties);
            writeLog("maimai 数据更新完成，共加载了 " + dataSize + "条数据");
            finishUpdate();
        } catch (Exception error) {
            writeLog("maimai 数据更新时出现错误:");
            onError(error);
        }
    }

    private int fetchMaimaiData(Set<DifficultyType> difficulties) {
        buildHttpClient(false);
        return batchFetchAndUploadData(difficulties);
    }

    private void loginWechat(String wechatAuthUrl) throws Exception {
        buildHttpClient(true);

        Log.d(TAG, wechatAuthUrl);

        Request request = new Request.Builder().addHeader("Host", "tgk-wcaime.wahlap.com").addHeader("Upgrade-Insecure-Requests", "1").addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12; IN2010 Build/RKQ1.211119.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/86.0.4240.99 XWEB/4317 MMWEBSDK/20220903 Mobile Safari/537.36 MMWEBID/363 MicroMessenger/8.0.28.2240(0x28001C57) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64").addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/wxpic,image/tpg,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9").addHeader("X-Requested-With", "com.tencent.mm").addHeader("Sec-Fetch-Site", "none").addHeader("Sec-Fetch-Mode", "navigate").addHeader("Sec-Fetch-User", "?1").addHeader("Sec-Fetch-Dest", "document").addHeader("Accept-Encoding", "gzip, deflate").addHeader("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7").get().url(wechatAuthUrl).build();

        Call call = client.newCall(request);
        Response response = call.execute();

        if (response.body() != null) {
            response.body().string();
        }
        Log.d(TAG, "登陆成功");

        int code = response.code();
        writeLog("登陆状态 " + code);
        if (code >= 400) {
            Exception exception = new Exception("登陆时出现错误，请重试！");
            onError(exception);
            throw new Exception(exception);
        }

        String location = response.headers().get("Location");
        if (response.code() >= 300 && location != null) {
            request = new Request.Builder().url(location).get().build();
            call = client.newCall(request);
            call.execute().close();
        }
    }

    private static void buildHttpClient(boolean followRedirect) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.connectTimeout(120, TimeUnit.SECONDS);
        builder.readTimeout(120, TimeUnit.SECONDS);
        builder.writeTimeout(120, TimeUnit.SECONDS);

        builder.followRedirects(followRedirect);
        builder.followSslRedirects(followRedirect);

        builder.cookieJar(jar);

        builder.cache(null);
        Interceptor noCacheInterceptor = chain -> {
            Request request = chain.request();
            Request.Builder builder1 = request.newBuilder().addHeader("Cache-Control", "no-cache");
            request = builder1.build();
            return chain.proceed(request);
        };
        builder.addInterceptor(noCacheInterceptor);

        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS).tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0).allEnabledCipherSuites().build();
        ConnectionSpec spec1 = new ConnectionSpec.Builder(ConnectionSpec.CLEARTEXT).build();
        builder.connectionSpecs(Arrays.asList(spec, spec1));

        builder.pingInterval(3, TimeUnit.SECONDS);

        client = builder.build();
    }
}
