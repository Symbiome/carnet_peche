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

import com.google.common.base.Preconditions;
import fr.inrae.fishola.database.NewsCarnetDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.StaffPerimeterDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.entities.tables.pojos.News;
import fr.inrae.fishola.entities.tables.pojos.NewsPicture;
import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.audit.Audited;
import fr.inrae.fishola.exceptions.NotAuthenticatedException;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.nuiton.util.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class NewsResource extends AbstractCarnetResource {

    @Inject
    protected NewsCarnetDao dao;

    @Inject
    protected ReferentialDao referentialDao;

    @Inject
    protected StaffPerimeterDao staffPerimeterDao;

    /**
     * Actualités publiées pour le pêcheur connecté : nationales, plus celles du
     * département de son code postal et de ses milieux favoris (#233). Sans
     * session pêcheur, nationales seules.
     */
    @GET
    @Path("/news")
    public List<News> getPublishedNews(@Context HttpServletRequest request) {
        return dao.getPublishedNewsForDepartments(currentAnglerDepartments());
    }

    /**
     * Actualités publiées sur un milieu : nationales, plus celles ciblant l'un
     * des départements du milieu (#233).
     */
    @GET
    @Path("/news/waterEntity/{waterEntityId}")
    public List<News> getPublishedNews(@PathParam("waterEntityId") UUID waterEntityId, @Context HttpServletRequest request) {
        return dao.getPublishedNewsForDepartments(dao.getWaterEntityDepartments(waterEntityId));
    }

    private Set<String> currentAnglerDepartments() {
        if (userToken == null) {
            return Set.of();
        }
        try {
            UUID userId = getUserIdOrRenew().userId();
            return usersDao.findById(userId).map(dao::getAnglerDepartments).orElse(Set.of());
        } catch (NotAuthenticatedException e) {
            return Set.of();
        }
    }

    @GET
    @Path("/news-all")
    public List<NewsBean> getAllNews(@Context HttpServletRequest request) {
        // Return all news
        FisholaAdmin currentAdmin = checkIsAdmin();
        return dao.getNews(false, Optional.of(currentAdmin)).stream().map(this::newsToNewsBean).toList();
    }
    @GET
    @Path("/news/{newsId}")
    public News getPublishedNews(@Context HttpServletRequest request, @PathParam("newsId") UUID newsId) {
       News news = dao.findById(newsId);
        LocalDateTime now = LocalDateTime.now();
       if (news == null || news.getDatePublicationDebut() == null || now.isBefore(news.getDatePublicationDebut()) ||
                news.getDatePublicationFin() == null || now.isAfter(news.getDatePublicationFin())) {
           throw new ResourceNotFoundException("News not found or not published yet");
       }
       return news;
    }


    @DELETE
    @Path("/news-all/{newsId}")
    @Audited(value = "news.delete", entityType = "news", entityIdParam = "newsId")
    public Response deleteNews(@PathParam("newsId") UUID newsId) {
        FisholaAdmin currentAdmin = checkIsAdmin();
        checkNewsInPerimeter(findExistingNews(newsId), currentAdmin);
        dao.deleteById(newsId);
        return Response.noContent().build();
    }

    @PUT
    @Path("/news-all/{newsId}")
    @Audited(value = "news.update", entityType = "news", entityIdParam = "newsId")
    public Response updateNews(@PathParam("newsId") UUID newsId, NewsBean news) {
        FisholaAdmin currentAdmin = checkIsAdmin();
        Preconditions.checkArgument(newsId != null, "Identifiant de news obligatoire");
        Preconditions.checkArgument(newsId.equals(news.id), "L'identifiant ne correspond pas");
        News existing = findExistingNews(newsId);
        checkNewsInPerimeter(existing, currentAdmin);
        try {
            dao.update(this.newsBeanToNews(news, existing.getIsNational()));
            return Response.noContent().build();
        } catch (Exception e) {
            Map<String, String> entity = new LinkedHashMap<>();
            entity.put("error", "Impossible de mettre à jour la news : " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }
    }


    @POST
    @Path("/news-all")
    @Audited(value = "news.create", entityType = "news")
    public Response createNews(NewsBean news) {
        FisholaAdmin currentAdmin = checkIsAdmin();
        boolean isNational = currentAdmin.getIsNationalAdmin();
        Set<String> departmentCodes = isNational ? Set.of() : getAllowedAdminDepartments();
        if (departmentCodes.contains(NO_DEPARTMENT_PERIMETER)) {
            throw new ForbiddenException("L'administrateur " + currentAdmin.getEmail() + " n'a aucun département de périmètre");
        }
        try {
            News inserted = dao.insert(this.newsBeanToNews(news, isNational), departmentCodes);
            // Update all news pictures uploaded with temp id
            dao.updateTempNewsPictureIds(inserted.getId());
            return Response.noContent().build();
        } catch (Exception e) {
            Map<String, String> entity = new LinkedHashMap<>();
            entity.put("error", "Impossible de créer la news : " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }
    }

    private News findExistingNews(UUID newsId) {
        News news = dao.findById(newsId);
        if (news == null) {
            throw new ResourceNotFoundException("News introuvable : " + newsId);
        }
        return news;
    }

    /**
     * Un admin régional ne modifie qu'une actu régionale dont tous les
     * départements relèvent de son périmètre (#159, #233). Un national passe
     * toujours ; une actu nationale est réservée au national.
     */
    protected void checkNewsInPerimeter(News news, FisholaAdmin currentAdmin) {
        if (currentAdmin.getIsNationalAdmin()) {
            return;
        }
        Set<String> newsDepartments = dao.getDepartmentCodes(news.getId());
        if (news.getIsNational() || !getAllowedAdminDepartments().containsAll(newsDepartments)) {
            throw new ForbiddenException("L'administrateur " + currentAdmin.getEmail()
                    + " n'a pas accès aux départements " + newsDepartments);
        }
    }

    @POST
    @Path("/news-picture/{newsId}")
    public NewsPicture postNewsPicture(@PathParam("newsId") String newsId, String content) {
        checkIsAdmin();
        try {
            return dao.insertNewsPicture(newsId, content, false);
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossible de créer l'image : " + e.getMessage());
        }
    }

    @POST
    @Path("/news-miniature/{newsId}")
    public NewsPicture postNewsMiniature(@PathParam("newsId") String newsId, String content) {
        checkIsAdmin();
        try {
            return dao.insertNewsPicture(newsId, content, true);
        } catch (Exception e) {
            throw new IllegalArgumentException("Impossible de créer l'image : " + e.getMessage());
        }
    }

    @GET
    @Path("/news-picture/{picId}")
    @Produces("image/jpeg")
    public Response getNewsPicture(@PathParam("picId") UUID picId) {
         Optional<byte[]> bytes = dao.getNewsPicture(picId);

        Response response = bytes.map(this::wrapAsStreamingOutput)
                .map(Response::ok)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND))
                .build();
        return response;
    }

    private NewsBean newsToNewsBean(News n) {
        Set<String> departmentCodes = this.dao.getDepartmentCodes(n.getId());
        return new NewsBean(n.getId(), n.getName(), n.getContent(), n.getDatePublicationDebut(), n.getDatePublicationFin(), n.getDateNotificationSent(), n.getMiniatureId(), n.getIsNational(), departmentCodes);
    }
    private News newsBeanToNews(NewsBean news, boolean isNational) {
        return new News(news.id, news.name, news.content, news.datePublicationDebut, news.datePublicationFin, news.dateNotificationSent, news.miniatureId, isNational);
    }
}
