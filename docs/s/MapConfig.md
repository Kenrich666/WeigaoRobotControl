```
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.keenon.sdk.sensor.map;

public class MapConfig {
    private String fileDirectory;
    private String fileName;
    private String fileMd5;
    private String type;
    private String mapUuid;

    public MapConfig(String fileDirectory, String fileName, String fileMd5, String type) {
        this.fileDirectory = fileDirectory;
        this.fileName = fileName;
        this.fileMd5 = fileMd5;
        this.type = type;
    }

    public MapConfig(String fileDirectory, String fileName, String fileMd5) {
        this.fileDirectory = fileDirectory;
        this.fileName = fileName;
        this.fileMd5 = fileMd5;
    }

    public MapConfig(String fileDirectory, String fileName, String fileMd5, String type, String mapUuid) {
        this.fileDirectory = fileDirectory;
        this.fileName = fileName;
        this.fileMd5 = fileMd5;
        this.type = type;
        this.mapUuid = mapUuid;
    }

    public MapConfig() {
    }

    public String getFileName() {
        return this.fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileDirectory() {
        return this.fileDirectory;
    }

    public void setFileDirectory(String fileDirectory) {
        this.fileDirectory = fileDirectory;
    }

    public String getFileMd5() {
        return this.fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMapUuid() {
        return this.mapUuid;
    }

    public void setMapUuid(String mapUuid) {
        this.mapUuid = mapUuid;
    }

    public String toString() {
        return "MapConfig{fileDirectory='" + this.fileDirectory + '\'' + ", fileName='" + this.fileName + '\'' + ", fileMd5='" + this.fileMd5 + '\'' + ", type='" + this.type + '\'' + ", mapUuid='" + this.mapUuid + '\'' + '}';
    }
}
```