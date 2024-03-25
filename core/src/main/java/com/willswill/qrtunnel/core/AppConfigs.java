package com.willswill.qrtunnel.core;

import lombok.Data;

/**
 * @author Will
 */
@Data
public class AppConfigs {
    private int chunkSize = 1000; // 每张图片所包含的数据大小，最大值是
    private int sendInterval = 200; // 发送端每张图片停顿时间（毫秒）

    private int imageWidth = 400; // 生成的图片宽度
    private int imageHeight = 400; // 生成的图片高度

    private String senderLayout; // 发送端图片布局

    private String saveDir; // 接收端文件保存路径
    private String rootPath; // 发送端文件相对路径的最上层
}
