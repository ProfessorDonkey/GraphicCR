/**
 * Copyright 2016 By_syk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.by_syk.graphiccr.core;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.by_syk.graphiccr.util.ExtraUtil;

/**
 * 第1类图形验证码识别
 * 
 * @author By_syk
 */
public class GraphicC1Translator {
    private static GraphicC1Translator translator = null;
    
    private static final int UNIT_W = 10;
    private static final int UNIT_H = 12;
    private static final int TARGET_COLOR = Color.BLACK.getRGB();
    private static final int USELESS_COLOR = Color.WHITE.getRGB();
    
    private GraphicC1Translator() {}
    
    public static GraphicC1Translator getInstance() {
        if (translator == null) {
            translator = new GraphicC1Translator();
        }
        
        return translator;
    }
    
    /**
     * 目标像素判断
     * （基于亮度）
     */
    private boolean isTarget(int colorInt) {
        Color color = new Color(colorInt);
        float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
        return hsb[2] < 0.6;
    }

    /**
     * 去噪
     */
    private BufferedImage denoise(File picFile) throws Exception {
        BufferedImage img = ImageIO.read(picFile);
        int width = img.getWidth();
        int height = img.getHeight();
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                if (isTarget(img.getRGB(x, y))) {
                    img.setRGB(x, y, TARGET_COLOR);
                } else {
                    img.setRGB(x, y, USELESS_COLOR);
                }
            }
        }
        return img;
    }

    /**
     * 分割元字符
     */
    private List<BufferedImage> split(BufferedImage img) throws Exception {
        List<BufferedImage> subImgs = new ArrayList<BufferedImage>();
        subImgs.add(img.getSubimage(5, 4, UNIT_W, UNIT_H));
        subImgs.add(img.getSubimage(18, 4, UNIT_W, UNIT_H));
        subImgs.add(img.getSubimage(32, 4, UNIT_W, UNIT_H));
        subImgs.add(img.getSubimage(44, 4, UNIT_W, UNIT_H));
        return subImgs;
    }
    
    /**
     * 取出训练数据
     */
    private Map<BufferedImage, Character> loadTrainData() throws Exception {
        Map<BufferedImage, Character> map = new HashMap<>();
        String trainLog = ExtraUtil.readFile(this.getClass().getResourceAsStream("/resources/img/1/1.txt"));
        if (trainLog == null) {
            return map;
        }
        for (String name : trainLog.split("\n")) {
            map.put(ImageIO.read(this.getClass().getResourceAsStream("/resources/img/1/"
                    + name)), name.charAt(0));
        }
        return map;
    }

    /**
     * 训练
     */
    public void train() {
        // 由于样本具有很强的规律性，已通过 PS 训练完成
    }

    /**
     * 单元识别
     */
    private char recognize(BufferedImage img, Map<BufferedImage, Character> trainImgMap) {
        char result = ' ';
        int width = img.getWidth();
        int height = img.getHeight();
        int min = width * height; // 最小差异像素数
        for (BufferedImage bi : trainImgMap.keySet()) {
            int count = 0; // 差异像素数
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    count += (img.getRGB(x, y) != bi.getRGB(x, y) ? 1 : 0);
                    if (count >= min) {
                        break;
                    }
                }
            }
            if (count < min) {
                min = count;
                result = trainImgMap.get(bi);
            }
        }
        return result;
    }

    /**
     * 图形验证码识别
     */
    public String translate(File file) {
        String result = "";
        try {
            BufferedImage img = denoise(file);
            List<BufferedImage> listImg = split(img);
            Map<BufferedImage, Character> map = loadTrainData();
            for (BufferedImage bi : listImg) {
                result += recognize(bi, map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}