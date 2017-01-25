package eu.daiad.web.repository.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import eu.daiad.web.domain.application.AccountAnnouncementEntity;
import eu.daiad.web.domain.application.AccountEntity;
import eu.daiad.web.domain.application.AnnouncementEntity;
import eu.daiad.web.domain.application.AnnouncementTranslationEntity;
import eu.daiad.web.model.PagingOptions;
import eu.daiad.web.model.message.Announcement;
import eu.daiad.web.repository.BaseRepository;

@Repository
@Transactional("applicationTransactionManager")
public class AccountAnnouncementRepository extends BaseRepository
    implements IAccountAnnouncementRepository
{
    public static final int DEFAULT_LIMIT = 50;

    @PersistenceContext(unitName = "default")
    EntityManager entityManager;

    @Override
    public AccountAnnouncementEntity findOne(int id)
    {
        return entityManager.find(AccountAnnouncementEntity.class, id);
    }

    @Override
    public AccountAnnouncementEntity findOne(UUID accountKey, int announcementId)
    {
        TypedQuery<AccountAnnouncementEntity> query = entityManager.createQuery(
            "SELECT a FROM account_announcement a WHERE " +
                "a.announcement.id = :aid AND a.account.key = :accountKey",
            AccountAnnouncementEntity.class);
        query.setParameter("aid", announcementId);
        query.setParameter("accountKey", accountKey);

        AccountAnnouncementEntity r = null;
        try {
            r = query.getSingleResult();
        } catch (NoResultException x) {
            r = null;
        }
        return r;
    }

    @Override
    public int countAll()
    {
        TypedQuery<Integer> query = entityManager.createQuery(
            "SELECT count(a.id) FROM account_announcement", Integer.class);
        return query.getSingleResult();
    }

    @Override
    public List<AccountAnnouncementEntity> findByAccount(UUID accountKey)
    {
        return findByAccount(accountKey, null);
    }

    @Override
    public List<AccountAnnouncementEntity> findByAccount(UUID accountKey, Interval interval)
    {
        TypedQuery<AccountAnnouncementEntity> query = entityManager.createQuery(
            "SELECT a FROM account_announcement a WHERE " +
                "a.account.key = :accountKey" +
                ((interval != null)? " AND a.createdOn >= :start AND a.createdOn < :end" : ""),
             AccountAnnouncementEntity.class);

        query.setParameter("accountKey", accountKey);
        if (interval != null) {
            query.setParameter("start", interval.getStart());
            query.setParameter("end", interval.getEnd());
        }
        return query.getResultList();
    }

    @Override
    public List<AccountAnnouncementEntity> findByAccount(UUID accountKey, int minId)
    {
        return findByAccount(accountKey, minId, new PagingOptions(DEFAULT_LIMIT));
    }

    @Override
    public List<AccountAnnouncementEntity> findByAccount(UUID accountKey, int minId, PagingOptions pagination)
    {
        TypedQuery<AccountAnnouncementEntity> query = entityManager.createQuery(
            "SELECT a FROM account_announcement a " +
                "WHERE a.account.key = :accountKey AND a.id > :minId " +
                "ORDER BY a.id " + (pagination.isAscending()? "ASC" : "DESC"),
            AccountAnnouncementEntity.class);

        query.setParameter("accountKey", accountKey);
        query.setParameter("minId", minId);

        int offset = pagination.getOffset();
        if (offset > 0)
            query.setFirstResult(offset);
        query.setMaxResults(pagination.getLimit());

        return query.getResultList();
    }

    @Override
    public int countByAccount(UUID accountKey)
    {
        return countByAccount(accountKey, (Interval) null);
    }

    @Override
    public int countByAccount(UUID accountKey, Interval interval)
    {
        TypedQuery<Integer> query = entityManager.createQuery(
            "SELECT count(a.id) FROM account_announcement a WHERE " +
                "a.account.key = :accountKey" +
                ((interval != null)? " AND a.createdOn >= :start AND a.createdOn < :end" : ""),
             Integer.class);

        query.setParameter("accountKey", accountKey);
        if (interval != null) {
            query.setParameter("start", interval.getStart());
            query.setParameter("end", interval.getEnd());
        }
        return query.getSingleResult().intValue();
    }

    @Override
    public int countByAccount(UUID accountKey, int minId)
    {
        TypedQuery<Integer> query = entityManager.createQuery(
            "SELECT count(a.id) FROM account_announcement a " +
                "WHERE a.account.key = :accountKey AND a.id > :minId ",
            Integer.class);
        query.setParameter("accountKey", accountKey);
        query.setParameter("minId", minId);
        return query.getSingleResult().intValue();
    }

    @Override
    public List<AccountAnnouncementEntity> findByAnnouncement(int announcementId)
    {
        return findByAnnouncement(announcementId, null);
    }

    @Override
    public List<AccountAnnouncementEntity> findByAnnouncement(int announcementId, Interval interval)
    {
        TypedQuery<AccountAnnouncementEntity> query = entityManager.createQuery(
            "SELECT a FROM account_announcement a WHERE " +
                "a.announcement.id = :aid" +
                ((interval != null)? " AND a.createdOn >= :start AND a.createdOn < :end" : ""),
            AccountAnnouncementEntity.class);

        query.setParameter("aid", announcementId);
        if (interval != null) {
            query.setParameter("start", interval.getStart());
            query.setParameter("end", interval.getEnd());
        }
        return query.getResultList();
    }

    @Override
    public int countByAnnouncement(int announcementId)
    {
        return countByAnnouncement(announcementId, null);
    }

    @Override
    public int countByAnnouncement(int announcementId, Interval interval)
    {
        TypedQuery<Integer> query = entityManager.createQuery(
            "SELECT count(a.id) FROM account_announcement a WHERE " +
                "a.announcement.id = :aid" +
                ((interval != null)? " AND a.createdOn >= :start AND a.createdOn < :end" : ""),
            Integer.class);

        query.setParameter("aid", announcementId);
        if (interval != null) {
            query.setParameter("start", interval.getStart());
            query.setParameter("end", interval.getEnd());
        }
        return query.getSingleResult().intValue();
    }

    @Override
    public AccountAnnouncementEntity create(AccountAnnouncementEntity e)
    {
        e.setCreatedOn(DateTime.now());
        entityManager.persist(e);
        return e;
    }

    @Override
    public AccountAnnouncementEntity createWith(AccountEntity account, int announcementId)
    {
        // Ensure we have a persistent AccountEntity instance
        if (!entityManager.contains(account))
            account = entityManager.find(AccountEntity.class, account.getId());

        AnnouncementEntity announcement =
            entityManager.find(AnnouncementEntity.class, announcementId);
        AccountAnnouncementEntity e =
            new AccountAnnouncementEntity(account, announcement);
        return create(e);
    }

    @Override
    public AccountAnnouncementEntity createWith(UUID accountKey, int announcementId)
    {
        TypedQuery<AccountEntity> query = entityManager.createQuery(
            "SELECT a FROM account a WHERE a.key = :accountKey", AccountEntity.class);
        query.setParameter("accountKey", accountKey);

        AccountEntity account;
        try {
            account = query.getSingleResult();
        } catch (NoResultException x) {
            account = null;
        }

        if (account == null)
            return null;
        else
            return createWith(account, announcementId);
    }

    @Override
    public boolean acknowledge(int id, DateTime acknowledged)
    {
        AccountAnnouncementEntity r = findOne(id);
        if (r != null)
            return acknowledge(r, acknowledged);
        return false;
    }

    @Override
    public boolean acknowledge(UUID accountKey, int id, DateTime acknowledged)
    {
        AccountAnnouncementEntity r = findOne(id);
        // Perform action only if message exists and is owned by account
        if (r != null && r.getAccount().getKey().equals(accountKey))
            return acknowledge(r, acknowledged);
        return false;
    }

    @Override
    public boolean acknowledge(AccountAnnouncementEntity r, DateTime acknowledged)
    {
        if (!entityManager.contains(r))
            r = findOne(r.getId());

        if (r != null && r.getAcknowledgedOn() == null) {
            r.setAcknowledgedOn(acknowledged);
            return true;
        }
        return false;
    }

    @Override
    public Announcement formatMessage(int id, Locale locale)
    {
        AccountAnnouncementEntity r = findOne(id);
        if (r != null)
            return formatMessage(r, locale);
        return null;
    }

    @Override
    public Announcement formatMessage(AccountAnnouncementEntity r, Locale locale)
    {
        AnnouncementEntity announcement = r.getAnnouncement();

        AnnouncementTranslationEntity translation = null;
        translation = announcement.getTranslation(locale);
        if (translation == null)
            translation = announcement.getTranslation(Locale.getDefault());
        if (translation == null)
            return null;

        Announcement message = new Announcement(r.getId());
        message.setPriority(announcement.getPriority());
        message.setTitle(translation.getTitle());
        message.setContent(translation.getContent());
        message.setCreatedOn(r.getCreatedOn().getMillis());
        if (r.getAcknowledgedOn() != null)
            message.setAcknowledgedOn(r.getAcknowledgedOn().getMillis());

        return message;
    }

    @Override
    public void delete(int id)
    {
        AccountAnnouncementEntity e =
            entityManager.find(AccountAnnouncementEntity.class, id);
        if (e != null)
            delete(e);
    }

    @Override
    public void delete(AccountAnnouncementEntity e)
    {
        entityManager.remove(e);
    }
}
