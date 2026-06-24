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
        String code = randomCode();
        session.setAttribute(CAPTCHA_CODE, code);
        session.setAttribute(CAPTCHA_TIME, System.currentTimeMillis());
        return Result.success(Map.of("image", drawBase64(code)));
    }

    /**
     * 随机生成指定长度的验证码字符串。
     */
    private String randomCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }

    /**
     * 把验证码绘制成 PNG，并叠加干扰线和噪点，最终编码为 data:image/png;base64。
     */
    private String drawBase64(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 浅绿背景，呼应系统整体配色。
        g.setColor(new Color(240, 249, 240));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线。
        for (int i = 0; i < 6; i++) {
            g.setColor(randomColor(120, 200));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }

        // 逐字绘制，带随机颜色与轻微旋转，增加机器识别难度。
        g.setFont(new Font("Arial", Font.BOLD, 28));
        int charWidth = WIDTH / (CODE_LENGTH + 1);
        for (int i = 0; i < code.length(); i++) {
            double angle = (random.nextDouble() - 0.5) * 0.5;
            int x = charWidth * (i + 1) - 6;
            int y = HEIGHT / 2 + 10;
            g.rotate(angle, x, y);
            g.setColor(randomColor(20, 110));
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-angle, x, y);
        }

        // 噪点。
        for (int i = 0; i < 60; i++) {
            g.setColor(randomColor(120, 220));
            g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
        }
        g.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("验证码生成失败", e);
        }
    }

    /**
     * 生成指定明度区间内的随机颜色，避免太亮看不清或太暗与背景冲突。
     */
    private Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(min + random.nextInt(range), min + random.nextInt(range), min + random.nextInt(range));
    }
}
