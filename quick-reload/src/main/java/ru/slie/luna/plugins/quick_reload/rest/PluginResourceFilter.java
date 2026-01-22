package ru.slie.luna.plugins.quick_reload.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.slie.luna.plugins.quick_reload.ResourceMapper;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Order(10)
public class PluginResourceFilter extends OncePerRequestFilter {
    ResourceMapper resourceMapper;

    public PluginResourceFilter(ResourceMapper resourceMapper) {
        this.resourceMapper = resourceMapper;
    }

    @Override
    @NullMarked
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        String uri = request.getRequestURI();

        for (String prefix: resourceMapper.getReplaces().keySet()) {
            if (uri.startsWith(prefix)) {
                Path path = Path.of(uri.replaceFirst(prefix, resourceMapper.getReplaces().get(prefix).toString()));
                System.out.println(path);
                if (!Files.isRegularFile(path)) {
                    continue;
                }

                String mime = URLConnection.guessContentTypeFromName(uri);
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                MediaType contentType = MediaType.parseMediaType(mime);

                response.setContentType(contentType.toString());
                response.setStatus(200);

                File file = path.toFile();
                response.setContentLengthLong(file.length());

                try (InputStream in = new FileInputStream(file);
                     OutputStream out = response.getOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                } catch (IOException ignored) {

                }


                return;
            }
        }
    }
}
