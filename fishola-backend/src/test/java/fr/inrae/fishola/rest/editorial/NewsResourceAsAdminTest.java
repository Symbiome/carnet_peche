package fr.inrae.fishola.rest.editorial;

/*-
 * #%L
 * Carnet de pêche :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.google.common.collect.Sets;
import fr.inrae.fishola.database.NewsCarnetDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.entities.tables.pojos.News;
import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.AbstractCarnetTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
class NewsResourceAsAdminTest extends AbstractCarnetTest {

    @Inject
    protected NewsCarnetDao newsDao;

    @Inject
    protected ReferentialDao referentialDao;

    private String token;

    @BeforeEach
    @Transactional
    void loginAsAdminAndCreateRandomNews() {
        token = loginAsAdmin();

        // Insert a non published news
        LocalDateTime now = LocalDateTime.now();
        News unpublishedNews = new News();
        unpublishedNews.setContent("Content");
        unpublishedNews.setName(this.token);
        unpublishedNews.setDatePublicationDebut(now.plusDays(10));
        unpublishedNews.setDatePublicationFin(now.plusDays(20));
        unpublishedNews.setIsNational(true);
        this.newsDao.insert(unpublishedNews, Set.of());

        // Insert a published news
        News publishedNews = new News();
        publishedNews.setContent("Content");
        publishedNews.setName("published-" + token);
        publishedNews.setDatePublicationDebut(now.minusDays(1));
        publishedNews.setDatePublicationFin(now.plusDays(10));
        publishedNews.setIsNational(true);
        this.newsDao.insert(publishedNews, Set.of());
    }

    @Test
    @Transactional
    void testGetAllNews() {
        // All news should be displayed no matter the publication date
        int expectedNewsCount = this.newsDao.getNews(false, Optional.empty()).size();
        Assertions.assertNotEquals(0, expectedNewsCount);
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .get("/api/v1/news-all")
                .then()
                .statusCode(200)
                .body("size()", equalTo(expectedNewsCount));
    }

    @Test
    @Transactional
    void testNewsUpdate() {
        // Update news's publication date
        News newsFromDB = newsDao.getNews(false, Optional.empty()).get(0);
        String modifiedName = "modified";
        NewsBean news = new NewsBean(newsFromDB.getId(), newsFromDB.getName(), newsFromDB.getContent(), newsFromDB.getDatePublicationDebut(), newsFromDB.getDatePublicationFin(), newsFromDB.getDateNotificationSent(), newsFromDB.getMiniatureId(), newsFromDB.getIsNational(), Sets.newLinkedHashSet());
        news.name = modifiedName;
        LocalDateTime now = LocalDateTime.now();
        news.datePublicationDebut = now.minusDays(1);
        news.datePublicationFin = now.plusDays(20);
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(news)
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .put("/api/v1/news-all/" + news.id)
                .then()
                .statusCode(204);
        News updated = newsDao.findById(news.id);
        Assertions.assertEquals(modifiedName, updated.getName());

        // TODO #11679 notifications should be sent due to modified date

        newsDao.deleteById(news.id);
    }

    @Test
    @Transactional
    void testNewsPost() {
        int newsCountBefore = this.newsDao.getNews(false, Optional.empty()).size();
        LocalDateTime now = LocalDateTime.now();
        NewsBean news = new NewsBean(UUID.randomUUID(),"published-newnews", "content", now.minusDays(1), now.plusDays(20), now, UUID.randomUUID(), false, Set.of());
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(news)
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .post("/api/v1/news-all")
                .then()
                .statusCode(204);

        int newsCountAfter = this.newsDao.getNews(false, Optional.empty()).size();

        // TODO #11679 notifications should be sent due to modified date
        Assertions.assertEquals(newsCountAfter, newsCountBefore + 1);
    }

    @Test
    @Transactional
    void testNewsDelete() {
        int newsCountBefore = this.newsDao.getNews(false, Optional.empty()).size();

        News news = newsDao.getNews(false, Optional.empty()).get(0);
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .delete("/api/v1/news-all/" + news.getId())
                .then()
                .statusCode(204);

        int newsCountAfter = this.newsDao.getNews(false, Optional.empty()).size();

        Assertions.assertEquals(newsCountAfter, newsCountBefore - 1);

    }

    @Test
    @Transactional
    void testPictureUploadedBeforeCreationIsAttachedToNews() throws IOException {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
        String pictureId = given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body("\"" + dataUrl + "\"")
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .post("/api/v1/news-picture/temp-id")
                .then()
                .statusCode(200)
                .extract().path("id");

        LocalDateTime now = LocalDateTime.now();
        NewsBean news = new NewsBean(null, "with-picture-" + token, "<img src=\"" + pictureId + "\">", now.minusDays(1), now.plusDays(20), null, null, false, null);
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body(news)
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .post("/api/v1/news-all")
                .then()
                .statusCode(204);

        News created = newsDao.getNews(false, Optional.empty()).stream()
                .filter(n -> n.getName().equals("with-picture-" + token)).findFirst().orElseThrow();
        given().when().get("/api/v1/news-picture/" + pictureId).then().statusCode(200);
        Assertions.assertTrue(created.getIsNational());
    }

    @Test
    void testUnreadablePictureIsRejected() {
        String dataUrl = "data:image/webp;base64," + Base64.getEncoder().encodeToString("not an image".getBytes());
        given()
                .when()
                .contentType(MediaType.APPLICATION_JSON)
                .body("\"" + dataUrl + "\"")
                .cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .post("/api/v1/news-picture/temp-id")
                .then()
                .statusCode(400);
    }

}
