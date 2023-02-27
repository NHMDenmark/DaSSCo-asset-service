package dk.northtech.dasscoassetservice.assets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet({"/assets/frontend-properties.json", "/assets/frontend-properties.js"})
public class FrontendPropertiesServlet extends HttpServlet {
  private final String frontendPropertiesJson;
  private final String frontendPropertiesJavaScript;

  @Inject
  public FrontendPropertiesServlet(FrontendProperties frontendProperties) {
    Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    // The configuration is by definition read-once on boot, so pre-serialize it:
    this.frontendPropertiesJson = gson.toJson(frontendProperties);
    this.frontendPropertiesJavaScript = "var frontendProperties = " + this.frontendPropertiesJson + ";";
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    boolean isJavaScript = req.getRequestURI().toLowerCase().endsWith(".js");
    res.setCharacterEncoding("utf-8");
    var origin = req.getHeader("Origin");
    if (origin != null && !origin.isBlank()) {
      res.setHeader("Access-Control-Allow-Origin", "*");
    }
    res.setHeader("Cache-Control", "no-cache");
    res.setContentType(isJavaScript ? "text/javascript" : "application/json");
    try (var w = res.getWriter()) {
      w.print(isJavaScript ? this.frontendPropertiesJavaScript : this.frontendPropertiesJson);
    }
  }
}
