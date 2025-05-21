package com.sismics.docs.rest.resource;

import com.sismics.docs.core.constant.PermType;
import com.sismics.docs.core.constant.UserRegistrationRequestStatus;
import com.sismics.docs.core.dao.UserDao;
import com.sismics.docs.core.dao.UserRegistrationRequestDao;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.docs.core.model.jpa.UserRegistrationRequest;
import com.sismics.docs.core.util.SecurityUtil;
import com.sismics.docs.core.util.jpa.PaginatedList;
import com.sismics.docs.core.util.jpa.PaginatedLists;
import com.sismics.docs.core.util.jpa.QueryParam;
import com.sismics.docs.core.util.jpa.SortCriteria;
import com.sismics.docs.rest.constant.BaseFunction;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.rest.util.ValidationUtil;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

/**
 * User registration request REST resources.
 *
 * @author Scott-Einstein
 */
@Path("/registrationrequest")
public class UserRegistrationRequestResource extends BaseResource {
    /**
     * Creates a new registration request.
     *
     * @param username Username
     * @param password Password
     * @param email Email
     * @return Response
     */
    @PUT
    @Produces("application/json")
    public Response register(
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("email") String email) {

        if (!authenticate()) {
            // Only unauthenticated users (guests) can register
            try {
                // Validate the input data
                ValidationUtil.validateRequired(username, "username");
                ValidationUtil.validateRequired(password, "password");
                ValidationUtil.validateRequired(email, "email");
                ValidationUtil.validateUsername(username, "username");
                ValidationUtil.validatePassword(password, "password");
                ValidationUtil.validateEmail(email, "email");

                // Check if the username is already taken
                UserDao userDao = new UserDao();
                User user = userDao.getActiveByUsername(username);
                if (user != null) {
                    throw new ClientException("AlreadyExistingUsername", "Username already exists");
                }

                // Check if a pending request already exists for this username
                UserRegistrationRequestDao registrationRequestDao = new UserRegistrationRequestDao();
                UserRegistrationRequest existingRequest = registrationRequestDao.getByUsername(username);
                if (existingRequest != null && existingRequest.getStatus().equals(UserRegistrationRequestStatus.PENDING.name())) {
                    throw new ClientException("AlreadyPendingRequest", "A registration request for this username is already pending");
                }

                // Create the registration request
                UserRegistrationRequest userRegistrationRequest = new UserRegistrationRequest();
                userRegistrationRequest.setUsername(username);
                userRegistrationRequest.setEmail(email);
                userRegistrationRequest.setPassword(SecurityUtil.hashPassword(password));

                registrationRequestDao.create(userRegistrationRequest);

                // Always return OK
                JsonObjectBuilder response = Json.createObjectBuilder()
                        .add("status", "ok");
                return Response.ok().entity(response.build()).build();

            } catch (Exception e) {
                if (e instanceof ClientException) {
                    throw (ClientException) e;
                } else {
                    throw new ServerException("RegistrationError", "Error registering a new user", e);
                }
            }
        } else {
            // Authenticated users cannot register
            throw new ClientException("AlreadyAuthenticated", "You are already authenticated");
        }
    }

    /**
     * Returns all registration requests.
     *
     * @param limit Page limit
     * @param offset Page offset
     * @param sortColumn Sort column
     * @param asc If true, ascending sorting, else descending
     * @param search Search query
     * @param status Filter by status
     * @return Response
     */
    @GET
    @Path("/list")
    @Produces("application/json")
    public Response list(
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset,
            @QueryParam("sort_column") String sortColumn,
            @QueryParam("asc") Boolean asc,
            @QueryParam("search") String search,
            @QueryParam("status") String status) {

        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        // Get the requests
        PaginatedList<UserRegistrationRequest> paginatedList = PaginatedLists.create(limit, offset);
        SortCriteria sortCriteria = new SortCriteria(sortColumn, asc);

        QueryParam queryParam = new QueryParam();
        queryParam.setSearch(search);
        queryParam.setStatus(status);

        UserRegistrationRequestDao registrationRequestDao = new UserRegistrationRequestDao();
//        registrationRequestDao.findByCriteria(paginatedList, queryParam, sortCriteria);
        // 修改一下：
        List<UserRegistrationRequest> requests = registrationRequestDao.findByCriteria(
                offset, limit, search, status, sortCriteria
        );
        paginatedList.setResultList(requests);
        paginatedList.setResultCount(registrationRequestDao.count(search, status));

        // Build the response
        JsonObjectBuilder response = Json.createObjectBuilder();
        JsonArrayBuilder requests = Json.createArrayBuilder();

        for (UserRegistrationRequest request : paginatedList.getResultList()) {
            requests.add(Json.createObjectBuilder()
                    .add("id", request.getId())
                    .add("username", request.getUsername())
                    .add("email", request.getEmail())
                    .add("create_date", request.getCreateDate().getTime())
                    .add("status", request.getStatus())
                    .add("process_date", request.getProcessDate() != null ? request.getProcessDate().getTime() : 0)
                    .add("notes", request.getNotes() != null ? request.getNotes() : ""));
        }

        response.add("total", paginatedList.getResultCount())
                .add("requests", requests);

        return Response.ok().entity(response.build()).build();
    }

    /**
     * Approves a registration request.
     *
     * @param id Request ID
     * @return Response
     */
    @POST
    @Path("{id: [a-z0-9\\-]+}/approve")
    @Produces("application/json")
    public Response approve(@PathParam("id") String id) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        // Get the request
        UserRegistrationRequestDao registrationRequestDao = new UserRegistrationRequestDao();
        UserRegistrationRequest request = registrationRequestDao.getById(id);
        if (request == null) {
            throw new ClientException("RegistrationRequestNotFound", "Registration request not found");
        }

        // Check if the request is still pending
        if (!request.getStatus().equals(UserRegistrationRequestStatus.PENDING.name())) {
            throw new ClientException("RegistrationRequestNotPending", "Registration request is not pending");
        }

        // Create the user
        UserDao userDao = new UserDao();
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword()); // Password is already hashed
        user.setEmail(request.getEmail());
        user.setCreateDate(new Date());
        user.setLocale(getLocale());

        try {
            // Create the user
            userDao.create(user, null);

            // Update the request status
            registrationRequestDao.approve(id);

            // Always return OK
            JsonObjectBuilder response = Json.createObjectBuilder()
                    .add("status", "ok");
            return Response.ok().entity(response.build()).build();

        } catch (Exception e) {
            throw new ServerException("ApprovalError", "Error approving the registration request", e);
        }
    }

    /**
     * Rejects a registration request.
     *
     * @param id Request ID
     * @param notes Rejection notes
     * @return Response
     */
    @POST
    @Path("{id: [a-z0-9\\-]+}/reject")
    @Produces("application/json")
    public Response reject(
            @PathParam("id") String id,
            @FormParam("notes") String notes) {

        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        // Get the request
        UserRegistrationRequestDao registrationRequestDao = new UserRegistrationRequestDao();
        UserRegistrationRequest request = registrationRequestDao.getById(id);
        if (request == null) {
            throw new ClientException("RegistrationRequestNotFound", "Registration request not found");
        }

        // Check if the request is still pending
        if (!request.getStatus().equals(UserRegistrationRequestStatus.PENDING.name())) {
            throw new ClientException("RegistrationRequestNotPending", "Registration request is not pending");
        }

        try {
            // Update the request status
            registrationRequestDao.reject(id, notes);

            // Always return OK
            JsonObjectBuilder response = Json.createObjectBuilder()
                    .add("status", "ok");
            return Response.ok().entity(response.build()).build();

        } catch (Exception e) {
            throw new ServerException("RejectionError", "Error rejecting the registration request", e);
        }
    }

    /**
     * Deletes a registration request.
     *
     * @param id Request ID
     * @return Response
     */
    @DELETE
    @Path("{id: [a-z0-9\\-]+}")
    @Produces("application/json")
    public Response delete(@PathParam("id") String id) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        // Get the request
        UserRegistrationRequestDao registrationRequestDao = new UserRegistrationRequestDao();
        UserRegistrationRequest request = registrationRequestDao.getById(id);
        if (request == null) {
            throw new ClientException("RegistrationRequestNotFound", "Registration request not found");
        }

        // Delete the request
        registrationRequestDao.delete(id);

        // Always return OK
        JsonObjectBuilder response = Json.createObjectBuilder()
                .add("status", "ok");
        return Response.ok().entity(response.build()).build();
    }
}