package dev.shuncha.headfireworkpaper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub Releasesから、HeadFirework(Paper版)の最新バージョンを確認するクラス。
 *
 * headfirework-paperはMOD版・OreHighlighterと同じ「minecraft-tools」モノレポに同居しているため、
 * GitHubの /releases/latest はそのまま使えない(他プロジェクトの方が新しいリリースだと、
 * そちらを最新として返してしまう)。そのため releases 一覧を取得し、タグ名が
 * "headfirework-paper-v" で始まるものだけに絞り込んでから最新版を判定している。
 */
public class UpdateChecker {

    private static final String RELEASES_API_URL =
            "https://api.github.com/repos/SHUN-CHACHA/minecraft-tools/releases";
    private static final String TAG_PREFIX = "headfirework-paper-v";
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    private final HeadFireworkPaperPlugin plugin;

    private volatile boolean updateAvailable = false;
    private volatile String latestVersion;
    private volatile String latestReleaseUrl;

    public UpdateChecker(HeadFireworkPaperPlugin plugin) {
        this.plugin = plugin;
    }

    /** サーバー起動時に一度だけ非同期で確認する。結果はこのインスタンスにキャッシュされる。*/
    public void checkAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::checkNow);
    }

    private void checkNow() {
        try {
            String currentVersion = plugin.getDescription().getVersion();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RELEASES_API_URL))
                    .header("User-Agent", "HeadFireworkPaper-UpdateCheck")
                    .header("Accept", "application/vnd.github+json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                plugin.getLogger().warning("更新チェックに失敗しました(HTTP " + response.statusCode() + ")。");
                return;
            }

            JsonArray releases = JsonParser.parseString(response.body()).getAsJsonArray();
            Optional<String[]> latest = findLatestForPrefix(releases, TAG_PREFIX);
            if (latest.isEmpty()) {
                return;
            }

            String remoteVersion = latest.get()[0];
            String releaseUrl = latest.get()[1];

            if (isNewer(remoteVersion, currentVersion)) {
                latestVersion = remoteVersion;
                latestReleaseUrl = releaseUrl;
                updateAvailable = true;
                plugin.getLogger().info("HeadFirework(Paper版)の新しいバージョン " + remoteVersion
                        + " が公開されています(現在: v" + currentVersion + ")。" + releaseUrl);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("更新チェック中にエラーが発生しました: " + e.getMessage());
        }
    }

    /** releases配列から指定タグprefixのものだけを見て、バージョンが最も新しい1件を返す。*/
    private Optional<String[]> findLatestForPrefix(JsonArray releases, String prefix) {
        String bestVersion = null;
        String bestUrl = null;
        for (JsonElement el : releases) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("draft") && obj.get("draft").getAsBoolean()) continue;
            if (!obj.has("tag_name")) continue;

            String tag = obj.get("tag_name").getAsString();
            if (tag == null || !tag.startsWith(prefix)) continue;

            String version = tag.substring(prefix.length());
            if (bestVersion == null || isNewer(version, bestVersion)) {
                bestVersion = version;
                bestUrl = obj.has("html_url") ? obj.get("html_url").getAsString() : null;
            }
        }
        return bestVersion == null ? Optional.empty() : Optional.of(new String[]{bestVersion, bestUrl});
    }

    /**
     * aがbより新しいバージョンかどうかを判定する(x.y.z形式で比較)。
     * 数値部分が同じ場合は、betaを含まない方(正式版)を新しいとみなす
     * (例: 1.0.1 は 1.0.1-beta.1 より新しい扱い)。
     */
    static boolean isNewer(String a, String b) {
        int[] va = parseVersion(a);
        int[] vb = parseVersion(b);
        for (int i = 0; i < 3; i++) {
            if (va[i] != vb[i]) return va[i] > vb[i];
        }
        boolean aBeta = a != null && a.toLowerCase().contains("beta");
        boolean bBeta = b != null && b.toLowerCase().contains("beta");
        return bBeta && !aBeta;
    }

    private static int[] parseVersion(String v) {
        int[] result = new int[3];
        if (v == null) return result;
        Matcher m = VERSION_PATTERN.matcher(v);
        if (m.find()) {
            for (int i = 0; i < 3; i++) {
                String g = m.group(i + 1);
                result[i] = g != null ? Integer.parseInt(g) : 0;
            }
        }
        return result;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getLatestReleaseUrl() {
        return latestReleaseUrl;
    }
}
