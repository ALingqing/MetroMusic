package top.chenray;

import com.xxmicloxx.NoteBlockAPI.model.Song;

import java.util.HashSet;
import java.util.Set;

/**
 * 歌曲包装类，包含歌曲元数据（播放次数、禁用状态、线路分配等）
 */
public class SongData {

    private final Song song;
    private final String fileName;
    private String displayName;
    private int playCount;
    private boolean disabled;
    private final Set<String> lines; // 允许播放此歌曲的地铁线路，空=所有线路

    public SongData(Song song, String fileName) {
        this.song = song;
        this.fileName = fileName;
        // displayName 仅取文件名（去掉子文件夹路径和 .nbs 后缀）
        String nameOnly = fileName.contains("/") || fileName.contains("\\")
                ? fileName.substring(fileName.lastIndexOf('/') + 1)
                : fileName;
        nameOnly = nameOnly.contains("\\")
                ? nameOnly.substring(nameOnly.lastIndexOf('\\') + 1)
                : nameOnly;
        this.displayName = nameOnly.endsWith(".nbs") || nameOnly.endsWith(".NBS")
                ? nameOnly.substring(0, nameOnly.length() - 4)
                : nameOnly;
        this.playCount = 0;
        this.disabled = false;
        this.lines = new HashSet<>();
    }

    public Song getSong() { return song; }
    public String getFileName() { return fileName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public int getPlayCount() { return playCount; }
    public void incrementPlayCount() { playCount++; }
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }
    public Set<String> getLines() { return lines; }
    public void addLine(String line) { lines.add(line); }
    public void removeLine(String line) { lines.remove(line); }
    public boolean isAllowedOnLine(String line) {
        return lines.isEmpty() || lines.contains(line);
    }
}
