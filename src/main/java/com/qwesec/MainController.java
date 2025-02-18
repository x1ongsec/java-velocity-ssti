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
 * @author X1ongSec
 * @date 2025/2/19 01:02
 */

@Controller
public class MainController {

    @RequestMapping("/evaluate/vul")
    @ResponseBody
    public String evaluateVul(@RequestParam(defaultValue="x1ongsec") String username) {
        String templateString = "Hello, " + username + " | Full name: $name, phone: $phone, email: $email";

        Velocity.init();
        VelocityContext ctx = new VelocityContext();
        ctx.put("name", "x1ong x1ong x1ong");
        ctx.put("phone", "012345678");
        ctx.put("email", "admin@qwesec.com");

        StringWriter out = new StringWriter();
        Velocity.evaluate(ctx, out, "test", templateString);

        return out.toString();
    }

    @RequestMapping("/merge/vul")
    @ResponseBody
    public String mergeVul(@RequestParam(defaultValue="x1ongsec") String username) throws IOException, ParseException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(String.valueOf(Paths.get(MainController.class.getClassLoader().getResource("templates/merge.vm").toString().replace("file:", "")))));

        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }

        String templateString = stringBuilder.toString();

        templateString = templateString.replace("<USERNAME>", username);

        StringReader reader = new StringReader(templateString);

        VelocityContext ctx = new VelocityContext();
        ctx.put("name", "x1ong x1ong x1ong");
        ctx.put("phone", "012345678");
        ctx.put("email", "admin@qwesec.com");

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
