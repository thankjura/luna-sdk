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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;

@Component
@Order(10)
public class PluginResourceFilter extends OncePerRequestFilter {
    ResourceMapper resourceMapper;

    public PluginResourceFilter(ResourceMapper resourceMapper) {
        this.resourceMapper = resourceMapper;
    }

    @Override
    @NullMarked
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException {
        String uri = request.getRequestURI();

        for (Map.Entry<String, Path> replace : resourceMapper.getReplaces().entrySet()) {
            String prefix = replace.getKey();
            if (!uri.startsWith(prefix + "/")) {
                continue;
            }

            Path root = replace.getValue().toAbsolutePath().normalize();
            // Resolve as a path, not as a regex replacement: Windows paths contain backslashes,
            // and ".." must not escape the resources directory
            Path path;
            try {
                path = root.resolve(uri.substring(prefix.length() + 1)).normalize();
            } catch (InvalidPathException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String mime = URLConnection.guessContentTypeFromName(path.getFileName().toString());
            if (mime == null) {
                mime = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            response.setContentType(MediaType.parseMediaType(mime).toString());
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLengthLong(Files.size(path));

            try (OutputStream out = response.getOutputStream()) {
                Files.copy(path, out);
                out.flush();
            }
            return;
        }
    }
}
