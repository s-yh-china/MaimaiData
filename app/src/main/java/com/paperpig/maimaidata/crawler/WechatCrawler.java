package com.paperpig.maimaidata.crawler;

import android.util.Log;
import androidx.core.util.Predicate;
import com.paperpig.maimaidata.db.entity.RecordEntity;
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity;
import com.paperpig.maimaidata.repository.RecordRepository;
import com.paperpig.maimaidata.repository.SongWithChartRepository;
import com.paperpig.maimaidata.utils.Constants;
import okhttp3.Call;
import okhttp3.ConnectionSpec;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.TlsVersion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.paperpig.maimaidata.crawler.CrawlerCaller.finishUpdate;
import static com.paperpig.maimaidata.crawler.CrawlerCaller.onError;
import static com.paperpig.maimaidata.crawler.CrawlerCaller.startAuth;
import static com.paperpig.maimaidata.crawler.CrawlerCaller.writeLog;

public class WechatCrawler {
    // Make this true for Fiddler to capture https request
    private static final boolean IGNORE_CERT = false;

    private static final int MAX_RETRY_COUNT = 4;

    private static final String TAG = "Crawler";

    private static final SimpleCookieJar jar = new SimpleCookieJar();

    private static final Map<Integer, String> diffMap = Map.of(
        0, "Basic",
        1, "Advance",
        2, "Expert",
        3, "Master",
        4, "Re:Master",
        10, "宴·会·场"
    );

    private static OkHttpClient client = null;

    private final RecordRepository recordRepository;
    private final SongWithChartRepository songWithChartRepository;

    public WechatCrawler(RecordRepository recordRepository, SongWithChartRepository songWithChartRepository) {
        buildHttpClient(false);
        this.recordRepository = recordRepository;
        this.songWithChartRepository = songWithChartRepository;
    }

    private int batchFetchAndUploadData(Set<Integer> difficulties) {
        List<CompletableFuture<List<RecordEntity>>> tasks = new ArrayList<>();
        List<RecordEntity> allRecords = new ArrayList<>();
        for (Integer diff : difficulties) {
            tasks.add(CompletableFuture.supplyAsync(() -> fetchAndUploadData(diff, 1)));
        }
        for (CompletableFuture<List<RecordEntity>> task : tasks) {
            allRecords.addAll(task.join());
        }
        Log.d(TAG, "共获取到" + allRecords.size() + "条数据");
        recordRepository.replaceAllRecord(allRecords);
        return allRecords.size();
    }

    private List<RecordEntity> fetchAndUploadData(Integer diff, Integer retryCount) {
        writeLog("开始获取 " + diffMap.get(diff) + " 难度的数据");
        Request request = new Request.Builder().url("https://maimai.wahlap.com/maimai-mobile/record/musicGenre/search/?genre=99&diff=" + diff).build();

        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            String data = Objects.requireNonNull(response.body()).string();
            Matcher matcher = Pattern.compile("<html.*>([\\s\\S]*)</html>").matcher(data);
            if (matcher.find()) {
                data = Objects.requireNonNull(matcher.group(1));
            }
            data = Pattern.compile("\\s+").matcher(data).replaceAll(" ");

            // Upload data to maimai-prober
            writeLog(diffMap.get(diff) + " 难度的数据已获取，正在处理");
            return parsePageToRecordList(data);
        } catch (Exception e) {
            return handleRetryFetchAndUploadData(e, diff, retryCount);
        }
    }

    private List<RecordEntity> parsePageToRecordList(String pageData) {
        Pattern diffPattern = Pattern.compile("diff_(.*)\\.png");
        List<RecordEntity> records = new ArrayList<>();
        Document doc = Jsoup.parse(pageData);

        Elements nameBlocks = doc.select("div.music_name_block.t_l.f_13.break");

        for (Element nameElement : nameBlocks) {
            try {
                String title = nameElement.text();

                if (title.isEmpty()) {
                    Log.d(TAG, "发现如月车站");
                    title = "　";
                }

                Element parentRow = nameElement.parent();
                if (parentRow == null) {
                    continue;
                }
                Elements siblings = parentRow.children();

                Element diffImg = null;
                for (Element sibling : siblings) {
                    Element img = sibling.select("img[src*=diff_]").first();
                    if (img != null) {
                        diffImg = img;
                        break;
                    }
                }

                if (diffImg == null) {
                    continue;
                }

                String diffSrc = diffImg.attr("src");
                Matcher diffMatcher = diffPattern.matcher(diffSrc);
                if (!diffMatcher.find()) {
                    continue;
                }

                String diffName = diffMatcher.group(1);
                int levelIndex = getLevelIndex(diffName);
                boolean isUtage = diffName.contains("utage");
                if (levelIndex == -1) {
                    continue;
                }

                Element superParentRow = nameElement.parent().parent().parent();
                Element typeImg = superParentRow.select("img.music_kind_icon").first();
                String type = Constants.CHART_TYPE_DX;
                if (typeImg != null) {
                    String src = typeImg.attr("src");
                    if (src.contains("standard")) {
                        type = Constants.CHART_TYPE_SD;
                    }
                } else if (isUtage) {
                    type = Constants.CHART_TYPE_UTAGE;
                } else {
                    Log.w(TAG, "无法获取铺面类型");
                }

                int song_id = -1;
                SongWithChartsEntity song_entity = null;
                List<SongWithChartsEntity> searchSongs = songWithChartRepository.searchSongsWithTitle(title);
                if (!searchSongs.isEmpty()) {
                    for (SongWithChartsEntity song : searchSongs) {
                        if (song.getSongData().getType().equals(type)) {
                            song_id = song.getSongData().getId();
                            song_entity = song;
                            break;
                        }
                    }
                }
                if (song_id == -1) {
                    Log.w(TAG, "无法获取 " + title + "(" + type + ") " + diffName + " 的歌曲数据");
                    writeLog("无法获取 " + title + "(" + type + ") " + diffName + " 的歌曲数据");
                    continue;
                }

                boolean isBuddy = isUtage && song_entity.getSongData().getBuddy() != null;

                String level = findText(siblings, "music_lv_block", null);
                String achievementText = findText(siblings, "music_score_block", null);
                String dxScoreText = findText(siblings, "music_score_block", s -> s.contains("/"));
                String fcIcon = findIcon(siblings, "fc");
                String fsIcon = findIcon(siblings, "fs");

                if (level == null || achievementText == null || dxScoreText == null) {
                    continue;
                }

                double achievements = Double.parseDouble(achievementText.replace("%", ""));
                int dxScore = Integer.parseInt(dxScoreText.split("/")[0].replace(",", "").replace(" ", ""));

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

                String fc = extractFromIcon(fcIcon);
                String fs = extractFromIcon(fsIcon);

                if ("fdx".equals(fs)) {
                    fs = "fsd";
                }
                if ("fdxp".equals(fs)) {
                    fs = "fsdp";
                }
                if ("back".equals(fc)) {
                    fc = "";
                }
                if ("back".equals(fs)) {
                    fs = "";
                }

                // Create record
                records.add(new RecordEntity(
                    0,
                    achievements,
                    dxScore,
                    fc,
                    fs,
                    level,
                    levelIndex,
                    rate,
                    song_id
                ));
                if (isBuddy) {
                    records.add(new RecordEntity(
                        0,
                        achievements,
                        dxScore,
                        fc,
                        fs,
                        level,
                        1,
                        rate,
                        song_id
                    ));
                }
            } catch (Exception e) {
                writeLog("解析记录时出错: " + e.getMessage());
                Log.w(TAG, "解析记录时出错: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        return records;
    }

    private static int getLevelIndex(String diffName) {
        return switch (diffName) {
            case "basic", "utage" -> 0;
            case "advanced" -> 1;
            case "expert" -> 2;
            case "master" -> 3;
            case "remaster" -> 4;
            default -> -1;
        };
    }

    private static String findText(Elements siblings, String clazz, Predicate<String> predicate) {
        for (Element sibling : siblings) {
            if (sibling.hasClass(clazz) && (predicate == null || predicate.test(sibling.text()))) {
                return sibling.text();
            }
        }
        return null;
    }

    private static String findIcon(Elements siblings, String iconName) {
        for (Element sibling : siblings) {
            Element img = sibling.select("img[src*=_icon_]").first();
            if (img != null) {
                String src = img.attr("src");
                if (src.contains(iconName)) {
                    return src;
                }
            }
        }
        return null;
    }

    private static final Pattern icoPattern = Pattern.compile("_icon_(.*)\\.png");

    private static String extractFromIcon(String iconSrc) {
        if (iconSrc == null) {
            return "";
        }
        Matcher m = icoPattern.matcher(iconSrc);
        return m.find() ? m.group(1) : "";
    }

    private List<RecordEntity> handleRetryFetchAndUploadData(Exception e, Integer diff, Integer currentRetryCount) {
        writeLog("获取 " + diffMap.get(diff) + " 难度数据时出现错误: " + e);
        if (currentRetryCount < MAX_RETRY_COUNT) {
            writeLog("进行第" + currentRetryCount + "次重试");
            return fetchAndUploadData(diff, currentRetryCount + 1);
        } else {
            writeLog(diffMap.get(diff) + "难度数据更新失败！");
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

    public void startFetch(Set<Integer> difficulties, String wechatAuthUrl) {
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

    private int fetchMaimaiData(Set<Integer> difficulties) {
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

        // Handle redirect manually
        String location = response.headers().get("Location");
        if (response.code() >= 300 && location != null) {
            request = new Request.Builder().url(location).get().build();
            call = client.newCall(request);
            call.execute().close();
        }
    }

    private static void buildHttpClient(boolean followRedirect) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (IGNORE_CERT) {
            ignoreCertBuilder(builder);
        }

        builder.connectTimeout(120, TimeUnit.SECONDS);
        builder.readTimeout(120, TimeUnit.SECONDS);
        builder.writeTimeout(120, TimeUnit.SECONDS);

        builder.followRedirects(followRedirect);
        builder.followSslRedirects(followRedirect);

        builder.cookieJar(jar);

        // No cache for http request
        builder.cache(null);
        Interceptor noCacheInterceptor = chain -> {
            Request request = chain.request();
            Request.Builder builder1 = request.newBuilder().addHeader("Cache-Control", "no-cache");
            request = builder1.build();
            return chain.proceed(request);
        };
        builder.addInterceptor(noCacheInterceptor);

        // Fix SSL handle shake error
        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS).tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0).allEnabledCipherSuites().build();
        // 兼容http接口
        ConnectionSpec spec1 = new ConnectionSpec.Builder(ConnectionSpec.CLEARTEXT).build();
        builder.connectionSpecs(Arrays.asList(spec, spec1));

        builder.pingInterval(3, TimeUnit.SECONDS);

        client = builder.build();
    }

    private static void ignoreCertBuilder(OkHttpClient.Builder builder) {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }};
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception ignored) {
        }
    }
}
