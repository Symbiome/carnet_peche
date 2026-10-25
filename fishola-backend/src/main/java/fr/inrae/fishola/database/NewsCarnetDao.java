package fr.inrae.fishola.database;

/*-
 * #%L
 * Carnet de pêche :: Backend
 * %%
 * Copyright (C) 2019 - 2023 INRAE - UMR CARRTEL
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

import fr.inrae.fishola.entities.Tables;
import fr.inrae.fishola.entities.tables.daos.NewsDao;
import fr.inrae.fishola.entities.tables.daos.NewsPictureDao;
import fr.inrae.fishola.entities.tables.daos.NextScheduledCourrielNotificationCheckDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.entities.tables.pojos.News;
import fr.inrae.fishola.entities.tables.pojos.NewsPicture;
import fr.inrae.fishola.entities.tables.pojos.NextScheduledCourrielNotificationCheck;
import fr.inrae.fishola.entities.tables.records.NewsRecord;
import fr.inrae.fishola.rest.ImageHelper;
import fr.inrae.fishola.rest.department.Departments;
import jakarta.inject.Singleton;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.impl.DAOImpl;
import org.jooq.impl.DSL;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Singleton
public class NewsCarnetDao extends AbstractCarnetDao {

    public static final UUID TEMPORARY_NEWS_ID = UUID.randomUUID();

    public List<News> getNews(boolean onlyListPublishedNews, Optional<FisholaAdmin> admin) {
        List<News> allNews = withDao(NewsDao.class, DAOImpl::findAll);
        if (onlyListPublishedNews) {
            LocalDateTime now = LocalDateTime.now();
            allNews = allNews.stream().filter(
                    news ->
                            news.getDatePublicationDebut() != null && now.isAfter(news.getDatePublicationDebut()) &&
                            news.getDatePublicationFin() != null && now.isBefore(news.getDatePublicationFin())
            ).toList();
        }
        if (admin.isPresent() && !admin.get().getIsNationalAdmin()) {
            // Admin régional : on ne garde que les actus ciblant au moins un de
            // ses départements de périmètre (#159, #233).
            Set<UUID> newsOfRelevantDepartments = withContext(context -> context
                    .selectDistinct(Tables.NEWS_DEPARTMENT.NEWS_ID)
                    .from(Tables.NEWS_DEPARTMENT)
                    .join(Tables.FISHOLA_ADMIN_DEPARTMENTS)
                        .on(Tables.FISHOLA_ADMIN_DEPARTMENTS.DEPARTMENT_CODE.eq(Tables.NEWS_DEPARTMENT.DEPARTMENT_CODE))
                    .where(Tables.FISHOLA_ADMIN_DEPARTMENTS.FISHOLA_ADMIN_ID.eq(admin.get().getId()))
                    .fetchSet(Tables.NEWS_DEPARTMENT.NEWS_ID));
            allNews = allNews.stream().filter(news ->
                    newsOfRelevantDepartments.contains(news.getId())
            ).toList();
        }
        allNews = allNews.stream().sorted(
                (n1, n2) -> -1 * n1.getDatePublicationDebut().compareTo(n2.getDatePublicationDebut())
        ).toList();
        return allNews;
    }

    /**
     * Actualités publiées visibles pour ces départements : les nationales et
     * celles qui ciblent au moins l'un d'eux (#233).
     *
     * @param departmentCodes départements du lecteur ; vide = nationales seules
     */
    public List<News> getPublishedNewsForDepartments(Set<String> departmentCodes) {
        LocalDateTime now = LocalDateTime.now();
        Condition targetsOneOfDepartments = DSL.exists(DSL.selectOne()
                .from(Tables.NEWS_DEPARTMENT)
                .where(Tables.NEWS_DEPARTMENT.NEWS_ID.eq(Tables.NEWS.ID))
                .and(Tables.NEWS_DEPARTMENT.DEPARTMENT_CODE.in(departmentCodes)));
        return withContext(context -> context
                .selectFrom(Tables.NEWS)
                .where(Tables.NEWS.IS_NATIONAL.isTrue().or(targetsOneOfDepartments))
                .and(Tables.NEWS.DATE_PUBLICATION_DEBUT.le(now))
                .and(Tables.NEWS.DATE_PUBLICATION_FIN.ge(now))
                .orderBy(Tables.NEWS.DATE_PUBLICATION_DEBUT.desc())
                .fetchInto(News.class));
    }

    /**
     * Départements d'un milieu, buffer staff compris (#231).
     *
     * @param waterEntityId milieu consulté
     */
    public Set<String> getWaterEntityDepartments(UUID waterEntityId) {
        return withContext(context -> context
                .select(Tables.WATER_ENTITY_DEPARTMENT.DEPARTMENT_CODE)
                .from(Tables.WATER_ENTITY_DEPARTMENT)
                .where(Tables.WATER_ENTITY_DEPARTMENT.WATER_ENTITY_ID.eq(waterEntityId))
                .fetchSet(Tables.WATER_ENTITY_DEPARTMENT.DEPARTMENT_CODE));
    }

    /**
     * Départements dont un pêcheur reçoit les actualités (#233) : celui de son
     * code postal et ceux de ses milieux favoris.
     *
     * @param user pêcheur lecteur ou destinataire du courriel
     */
    public Set<String> getAnglerDepartments(FisholaUser user) {
        Set<String> departments = new HashSet<>(withContext(context -> context
                .selectDistinct(Tables.WATER_ENTITY_DEPARTMENT.DEPARTMENT_CODE)
                .from(Tables.WATER_ENTITY_DEPARTMENT)
                .join(Tables.FISHOLA_USER_FAVORITE_WATER_ENTITIES)
                    .on(Tables.FISHOLA_USER_FAVORITE_WATER_ENTITIES.WATER_ENTITY_ID.eq(Tables.WATER_ENTITY_DEPARTMENT.WATER_ENTITY_ID))
                .where(Tables.FISHOLA_USER_FAVORITE_WATER_ENTITIES.FISHOLA_USER_ID.eq(user.getId()))
                .fetchSet(Tables.WATER_ENTITY_DEPARTMENT.DEPARTMENT_CODE)));
        Departments.fromPostalCode(user.getPostalCode()).ifPresent(departments::add);
        return departments;
    }

    public void deleteById(UUID newsId) {
        // Delete pics associated to this news
        DSLContext context = newContext();
        context.deleteFrom(Tables.NEWS_PICTURE).where(Tables.NEWS_PICTURE.NEWS_ID.eq(newsId)).execute();
        // Delete news itself
        withDaoNoResult(NewsDao.class, dao -> dao.deleteById(newsId));
    }

    public void update(News news) {
        withDaoNoResult(NewsDao.class, dao -> dao.update(news));
    }

    public News insert(News news, Set<String> departmentCodes) {
        DSLContext context = newContext();
        NewsRecord newRecord = context.newRecord(Tables.NEWS, news);
        News inserted = context.insertInto(Tables.NEWS)
                .set(newRecord)
                .returningResult(Tables.NEWS.ID)
                .fetchOne()
                .into(News.class);
        departmentCodes.forEach(departmentCode -> context
                .insertInto(Tables.NEWS_DEPARTMENT)
                .columns(Tables.NEWS_DEPARTMENT.NEWS_ID, Tables.NEWS_DEPARTMENT.DEPARTMENT_CODE)
                .values(inserted.getId(), departmentCode)
                .execute());
        return inserted;
    }

    public NewsPicture insertNewsPicture(String newsId, String content, boolean isMiniature) {
        DSLContext dslContext = newContext();
        NewsPicture newsPicture = new NewsPicture();
        newsPicture.setIsMiniature(isMiniature);
        // Step 1: convert content to JPEG base 64 string
        String[] contentSplitted = content.split(",");
        String base64Image = contentSplitted[1].replace("\"", "");
        byte[] jpegBytes = ImageHelper.base64ImageToJpegBytes(base64Image, config.rawImageQuality());
        newsPicture.setContent(jpegBytes);

        // Step 2: assign temporary news id (for picture associated to not yet posted pic)
        if (newsId.equals("temp-id")) {
            newsPicture.setNewsId(TEMPORARY_NEWS_ID);
        } else {
            newsPicture.setNewsId(UUID.fromString(newsId));
        }

        // If miniature, delete previous one
        if (isMiniature) {
            dslContext.deleteFrom(Tables.NEWS_PICTURE).where(Tables.NEWS_PICTURE.NEWS_ID.eq(newsPicture.getNewsId()).and(Tables.NEWS_PICTURE.IS_MINIATURE.eq(true))).execute();
        }

        // Step 3: create pic and return id
        final NewsPicture inserted =  dslContext
                .insertInto(Tables.NEWS_PICTURE)
                .columns(Tables.NEWS_PICTURE.CONTENT, Tables.NEWS_PICTURE.NEWS_ID, Tables.NEWS_PICTURE.IS_MINIATURE)
                .values(newsPicture.getContent(), newsPicture.getNewsId(), isMiniature)
                .returningResult(Tables.NEWS_PICTURE.ID)
                .fetchOne()
                .into(NewsPicture.class);
        NewsPicture picture = withDao(NewsPictureDao.class, dao -> dao.findById(inserted.getId()));
        if (picture.getIsMiniature()) {
            // If miniature, update news to indicate url
            News news = findById(picture.getNewsId());
            if (news != null) {
                this.updateMiniatureId(news, picture.getId());
            }
        }

        return picture;

    }

    private void updateMiniatureId(News news, UUID miniatureId) {
        news.setMiniatureId(miniatureId);
        withDaoNoResult(NewsDao.class, dao -> dao.update(news));
    }

    public void updateTempNewsPictureIds(UUID newsId) {
        DSLContext dslContext = newContext();
        dslContext
            .update(Tables.NEWS_PICTURE)
            .set(Tables.NEWS_PICTURE.NEWS_ID, newsId)
            .where(Tables.NEWS_PICTURE.NEWS_ID.eq(TEMPORARY_NEWS_ID))
            .execute();

        List<Record1<UUID>> miniature = dslContext.select(Tables.NEWS_PICTURE.ID)
                .from(Tables.NEWS_PICTURE).where(Tables.NEWS_PICTURE.NEWS_ID.eq(newsId).and(Tables.NEWS_PICTURE.IS_MINIATURE
                .eq(true))).fetch().stream().toList();
        if (!miniature.isEmpty()) {
            News news = findById(newsId);
            if (news != null) {
                this.updateMiniatureId(news, miniature.get(0).component1());
            }
        }
    }

    public News findById(UUID newsId) {
        return withDao(NewsDao.class, dao -> dao.findById(newsId));
    }

    public Optional<byte[]> getNewsPicture(UUID picId) {
        NewsPicture picture = withDao(NewsPictureDao.class, dao -> dao.findById(picId));
        Optional<byte[]> result = Optional.ofNullable(picture).map(NewsPicture::getContent);
        return result;
    }

    public NextScheduledCourrielNotificationCheck getNextScheduledNotificationCheck() {
        List<NextScheduledCourrielNotificationCheck> nextScheduledCourrielNotificationCheckDates = withDao(NextScheduledCourrielNotificationCheckDao.class, DAOImpl::findAll);
        if (nextScheduledCourrielNotificationCheckDates.isEmpty()) {
            // If no date defined, schedule for tomorrow at 7h30
            LocalDateTime nextSchedule = LocalDateTime.now();
            nextSchedule = nextSchedule.plusDays(1).withHour(7).withMinute(30).withSecond(0);
            NextScheduledCourrielNotificationCheck nextCheck = new NextScheduledCourrielNotificationCheck();
            nextCheck.setNextCheckDate(nextSchedule);
            withDaoNoResult(NextScheduledCourrielNotificationCheckDao.class, dao -> dao.insert(nextCheck));
            return nextCheck;
        } else {
            return nextScheduledCourrielNotificationCheckDates.iterator().next();
        }
    }

    public void scheduleNestNotificationCheck(int newsMailSendingDelayHours) {
        NextScheduledCourrielNotificationCheck nextScheduledNotificationCheck = getNextScheduledNotificationCheck();
        nextScheduledNotificationCheck.setNextCheckDate(nextScheduledNotificationCheck.getNextCheckDate().plusHours(newsMailSendingDelayHours));
        withDaoNoResult(NextScheduledCourrielNotificationCheckDao.class, dao -> dao.update(nextScheduledNotificationCheck));
    }

    public Set<String> getDepartmentCodes(UUID newsId) {
        return withContext(context -> context
                .select(Tables.NEWS_DEPARTMENT.DEPARTMENT_CODE)
                .from(Tables.NEWS_DEPARTMENT)
                .where(Tables.NEWS_DEPARTMENT.NEWS_ID.eq(newsId))
                .fetchSet(Tables.NEWS_DEPARTMENT.DEPARTMENT_CODE));
    }

    public void notifySent(News news) {
        news.setDateNotificationSent(LocalDateTime.now());
        withDaoNoResult(NewsDao.class, dao -> dao.update(news));
    }
}
