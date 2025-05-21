package com.sismics.docs.core.dao;

import com.google.common.base.Strings;
import com.sismics.docs.core.constant.UserRegistrationRequestStatus;
import com.sismics.docs.core.model.jpa.UserRegistrationRequest;
import com.sismics.docs.core.util.jpa.SortCriteria;
import com.sismics.util.context.ThreadLocalContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.util.*;

/**
 * User registration request DAO.
 *
 * @author Scott-Einstein
 */
public class UserRegistrationRequestDao {
    /**
     * Creates a new registration request.
     *
     * @param userRegistrationRequest User registration request
     * @return ID of the created request
     */
    public String create(UserRegistrationRequest userRegistrationRequest) {
        // Generate ID
        userRegistrationRequest.setId(UUID.randomUUID().toString());

        // Set creation date
        userRegistrationRequest.setCreateDate(new Date());

        // Set status to PENDING
        userRegistrationRequest.setStatus(UserRegistrationRequestStatus.PENDING.name());

        // Create the request
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        em.persist(userRegistrationRequest);

        return userRegistrationRequest.getId();
    }

    /**
     * Updates a registration request.
     *
     * @param userRegistrationRequest User registration request
     */
    public void update(UserRegistrationRequest userRegistrationRequest) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        em.merge(userRegistrationRequest);
    }

    /**
     * Approves a registration request.
     *
     * @param id Request ID
     */
    public void approve(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        UserRegistrationRequest request = em.find(UserRegistrationRequest.class, id);
        if (request != null) {
            request.setStatus(UserRegistrationRequestStatus.APPROVED.name());
            request.setProcessDate(new Date());
            em.merge(request);
        }
    }

    /**
     * Rejects a registration request.
     *
     * @param id Request ID
     * @param notes Rejection notes
     */
    public void reject(String id, String notes) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        UserRegistrationRequest request = em.find(UserRegistrationRequest.class, id);
        if (request != null) {
            request.setStatus(UserRegistrationRequestStatus.REJECTED.name());
            request.setProcessDate(new Date());
            request.setNotes(notes);
            em.merge(request);
        }
    }

    /**
     * Gets a registration request by its ID.
     *
     * @param id Request ID
     * @return User registration request
     */
    public UserRegistrationRequest getById(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        try {
            return em.find(UserRegistrationRequest.class, id);
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Gets a registration request by username.
     *
     * @param username Username
     * @return User registration request or null
     */
    public UserRegistrationRequest getByUsername(String username) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query q = em.createQuery("select r from UserRegistrationRequest r where r.username = :username");
        q.setParameter("username", username);
        try {
            return (UserRegistrationRequest) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Finds registration requests by criteria.
     *
     * @param offset 起始位置
     * @param limit 返回条数
     * @param search 搜索关键词
     * @param status 状态过滤
     * @param sortCriteria 排序条件（忽略此参数）
     * @return List of registration requests
     */
    public List<UserRegistrationRequest> findByCriteria(int offset, int limit, String search, String status, Object sortCriteria) {
        Map<String, Object> parameterMap = new HashMap<>();
        StringBuilder sb = new StringBuilder("select r from UserRegistrationRequest r");

        // Add search criteria
        List<String> criteriaList = new ArrayList<>();
        if (!Strings.isNullOrEmpty(search)) {
            criteriaList.add("(r.username like :search or r.email like :search)");
            parameterMap.put("search", "%" + search + "%");
        }
        if (!Strings.isNullOrEmpty(status)) {
            criteriaList.add("r.status = :status");
            parameterMap.put("status", status);
        }

        if (!criteriaList.isEmpty()) {
            sb.append(" where ");
            sb.append(String.join(" and ", criteriaList));
        }

        // 简单的默认排序
        sb.append(" order by r.createDate desc");

        // Create and execute the query
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query q = em.createQuery(sb.toString());
        for (Map.Entry<String, Object> entry : parameterMap.entrySet()) {
            q.setParameter(entry.getKey(), entry.getValue());
        }
        q.setFirstResult(offset);
        q.setMaxResults(limit);

        @SuppressWarnings("unchecked")
        List<UserRegistrationRequest> requestList = q.getResultList();
        return requestList;
    }

    /**
     * Returns the total number of registration requests.
     *
     * @param search 搜索关键词
     * @param status 状态过滤
     * @return Number of registration requests
     */
    public int count(String search, String status) {
        Map<String, Object> parameterMap = new HashMap<>();
        StringBuilder sb = new StringBuilder("select count(r.id) from UserRegistrationRequest r");

        // Add search criteria
        List<String> criteriaList = new ArrayList<>();
        if (!Strings.isNullOrEmpty(search)) {
            criteriaList.add("(r.username like :search or r.email like :search)");
            parameterMap.put("search", "%" + search + "%");
        }
        if (!Strings.isNullOrEmpty(status)) {
            criteriaList.add("r.status = :status");
            parameterMap.put("status", status);
        }

        if (!criteriaList.isEmpty()) {
            sb.append(" where ");
            sb.append(String.join(" and ", criteriaList));
        }

        // Create and execute the query
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query q = em.createQuery(sb.toString());
        for (Map.Entry<String, Object> entry : parameterMap.entrySet()) {
            q.setParameter(entry.getKey(), entry.getValue());
        }

        return ((Long) q.getSingleResult()).intValue();
    }

    /**
     * Deletes a registration request.
     *
     * @param id Request ID
     */
    public void delete(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        UserRegistrationRequest request = em.find(UserRegistrationRequest.class, id);
        if (request != null) {
            em.remove(request);
        }
    }
}