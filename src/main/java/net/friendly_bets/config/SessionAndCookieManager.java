package net.friendly_bets.config;

import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class SessionAndCookieManager {

    public void createSessionAndCookie(HttpServletRequest request, HttpServletResponse response, String attributeName, String attributeValue) {
        HttpSession session = request.getSession(true);
        session.setAttribute(attributeName, attributeValue);

        Cookie cookie = new Cookie("customCookie", "cookieValue");
        cookie.setMaxAge(3600); // Время жизни куки в секундах
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public String getSessionAttribute(HttpServletRequest request, String attributeName) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (String) session.getAttribute(attributeName);
        }
        return null;
    }
}
