package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.util.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * 图形验证码接口。
 *
 * 找回密码等无登录态场景下，先让用户识别一张随机字符图片，校验通过才允许继续操作，
 * 以此挡住脚本批量猜测用户名/手机号。验证码答案只存在服务端 Session（不下发给前端），
 * 前端拿到的仅是图片，避免答案在网络中暴露。
 */
@RestController
@RequestMapping("/api")
public class CaptchaController {

    /** Session 中保存验证码答案与生成时间的键。 */
    public static final String CAPTCHA_CODE = "CAPTCHA_CODE";
    public static final String CAPTCHA_TIME = "CAPTCHA_TIME";

    /** 验证码有效期：5 分钟。 */
    public static final long CAPTCHA_TTL_MILLIS = 5 * 60 * 1000L;

    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;
    private static final int CODE_LENGTH = 4;
    // 去掉容易混淆的 0/O、1/I/L 等字符，降低用户误读。
    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();

    private final SecureRandom random = new SecureRandom();

    /**
     * 生成一张验证码图片，答案写入 Session，图片以 base64 返回供前端直接渲染。
     */
    @GetMapping("/captcha")
    public Result<?> captcha(HttpSession session) {
        // 生成随机验证码字符串，答案只保存在服务端 Session 中，不随图片一起返回给前端。
        String code = randomCode();
        // CAPTCHA_CODE：验证时与用户输入比对的期望答案。
        session.setAttribute(CAPTCHA_CODE, code);
        // CAPTCHA_TIME：验证码生成的毫秒时间戳，用于判断 5 分钟有效期。
        session.setAttribute(CAPTCHA_TIME, System.currentTimeMillis());
        // 返回 base64 图片，前端用 <img src="data:image/png;base64,..."> 直接渲染，无需再次请求。
        return Result.success(Map.of("image", drawBase64(code)));
    }

    /**
     * 随机生成指定长度的验证码字符串。
     */
    private String randomCode() {
        // 预分配 StringBuilder 容量避免扩容。
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            // random.nextInt(ALPHABET.length) 产生 [0, ALPHABET.length) 的随机整数，
            // 用来从字母表中随机取一个字符。SecureRandom 比 Random 更难被预测。
            builder.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }

    /**
     * 把验证码绘制成 PNG，并叠加干扰线和噪点，最终编码为 data:image/png;base64。
     */
    private String drawBase64(String code) {
        // TYPE_INT_RGB：每像素用 32 位 int 表示 RGB（无透明通道），适合不透明验证码图片。
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        // Graphics2D 是画布对象，所有绘制操作都通过它完成。
        Graphics2D g = image.createGraphics();
        // 开启抗锯齿：字符边缘更平滑，不会出现锯齿感，有助于视觉上更清晰。
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 用浅绿色填满整个画布作为背景，呼应系统整体配色。
        g.setColor(new Color(240, 249, 240));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 绘制 6 条随机干扰线，颜色在中等亮度范围（120~200），不能太浅（看不出）或太深（掩盖字符）。
        for (int i = 0; i < 6; i++) {
            g.setColor(randomColor(120, 200));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }

        // 设置字体：Arial 粗体 28px，字符足够大便于人眼识别。
        g.setFont(new Font("Arial", Font.BOLD, 28));
        // charWidth：把宽度均匀分成 CODE_LENGTH+1 格，每个字符居中在一格里。
        int charWidth = WIDTH / (CODE_LENGTH + 1);
        for (int i = 0; i < code.length(); i++) {
            // angle：[-0.25, 0.25] 弧度的随机旋转，使字符轻微倾斜增加 OCR 难度。
            double angle = (random.nextDouble() - 0.5) * 0.5;
            // x：第 i 个字符的横坐标，均匀分布在画布宽度上；-6 是微调使字符居中。
            int x = charWidth * (i + 1) - 6;
            // y：字符基线高度，HEIGHT/2+10 约在画布垂直中间偏下。
            int y = HEIGHT / 2 + 10;
            // rotate 以 (x, y) 为旋转中心，避免字符整体位移。
            g.rotate(angle, x, y);
            // 字符颜色深（20~110），与浅绿背景形成对比，人眼易读而 OCR 较难。
            g.setColor(randomColor(20, 110));
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            // 绘制完当前字符后撤销旋转，避免影响下一个字符的坐标系。
            g.rotate(-angle, x, y);
        }

        // 绘制 60 个 1×1 像素的噪点，进一步干扰自动识别。
        for (int i = 0; i < 60; i++) {
            g.setColor(randomColor(120, 220));
            g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
        }
        // dispose 释放画布系统资源，避免内存泄漏。
        g.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // 将 BufferedImage 编码为 PNG 字节流写入内存缓冲区。
            ImageIO.write(image, "png", out);
            // 将字节数组转 Base64 字符串，拼上 data URL 前缀，前端 <img> 标签可直接使用。
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("验证码生成失败", e);
        }
    }

    /**
     * 生成指定明度区间内的随机颜色，避免太亮看不清或太暗与背景冲突。
     */
    private Color randomColor(int min, int max) {
        // range = max - min，保证 nextInt(range) 产生 [0, range) 的随机数，加上 min 后在 [min, max) 内。
        int range = max - min;
        // 三个通道（R/G/B）各自独立随机，产生随机色；三通道相近时接近灰色，差异大时产生彩色。
        return new Color(min + random.nextInt(range), min + random.nextInt(range), min + random.nextInt(range));
    }
}
