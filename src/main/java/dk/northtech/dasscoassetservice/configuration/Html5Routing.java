package dk.northtech.dasscoassetservice.configuration;

import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * This class sets up the "redirect 404 to index.html" which is needed for HTML5 routing as used by, say, the Angular
 * router.
 */
@Configuration
public class Html5Routing {
  @Bean
  ErrorViewResolver redirect404ToIndex() {
    return (request, status, model) -> {
      if (status == HttpStatus.NOT_FOUND) {
        Object p = model.get("path");
        if (p instanceof String) {
          var path = ((String) p).toLowerCase();
          // Don't redirect errors under the /api/ or /assets/ path, since these are known, reserved paths which can't be angular routes.
          if (!path.startsWith("/api/") && !path.startsWith("/assets/") && !path.startsWith("/openapi.json") && !path.startsWith("/openapi.yaml")) {
            return new ModelAndView("index.html", HttpStatus.OK);
          }
        }
      }
      return null;
    };
  }
}
