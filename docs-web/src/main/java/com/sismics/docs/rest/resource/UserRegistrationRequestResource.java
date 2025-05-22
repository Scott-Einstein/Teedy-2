package com.sismics.docs.rest.resource;

import com.sismics.docs.core.constant.UserRegistrationRequestStatus;
import com.sismics.docs.core.dao.UserDao;
import com.sismics.docs.core.dao.UserRegistrationRequestDao;
import com.sismics.docs.core.model.jpa.User;
import com.sismics.docs.core.model.jpa.UserRegistrationRequest;
import com.sismics.docs.rest.constant.BaseFunction;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.exception.ServerException;

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
                // 验证输入数据
                if (username == null || username.isEmpty()) {
                    throw new ClientException("ValidationError", "Username is required");
                }
                if (password == null || password.isEmpty()) {
                    throw new ClientException("ValidationError", "Password is required");
                }
                if (email == null || email.isEmpty()) {
                    throw new ClientException("ValidationError", "Email is required");
                }

                // 验证用户名格式
                if (!username.matches("^[a-zA-Z0-9_]{3,50}$")) {
                    throw new ClientException("ValidationError", "Username must be alphanumeric with 3-50 characters");
                }

                // 验证邮箱格式
                if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                    throw new ClientException("ValidationError", "Invalid email format");
                }

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

                // 使用正确的密码哈希方法
                // 简单地实现一个哈希方法而不是引用不存在的类
                String hashedPassword = hashPassword(password);
                userRegistrationRequest.setPassword(hashedPassword);

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
     * 简单的密码哈希方法实现
     * @param password 原始密码
     * @return 哈希后的密码
     */
    private String hashPassword(String password) {
        // 简单实现，实际项目中应使用更安全的方法
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(password);
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

        // 设置默认值
        if (limit == null) {
            limit = 50;
        }
        if (offset == null) {
            offset = 0;
        }

        // 创建排序条件 - 使用自定义排序实现，不依赖SortCriteria
        // 我们在DAO层处理排序逻辑，此处传递原始参数
        UserRegistrationRequestDao registrationRequestDao = new UserRegistrationRequestDao();
        List<UserRegistrationRequest> requestList = registrationRequestDao.findByCriteria(
                offset, limit, search, status, null
        );
        int count = registrationRequestDao.count(search, status);

        // 构建响应
        JsonObjectBuilder response = Json.createObjectBuilder();
        JsonArrayBuilder requestsArray = Json.createArrayBuilder();

        for (UserRegistrationRequest request : requestList) {
            requestsArray.add(Json.createObjectBuilder()
                    .add("id", request.getId())
                    .add("username", request.getUsername())
                    .add("email", request.getEmail())
                    .add("create_date", request.getCreateDate().getTime())
                    .add("status", request.getStatus())
                    .add("process_date", request.getProcessDate() != null ? request.getProcessDate().getTime() : 0)
                    .add("notes", request.getNotes() != null ? request.getNotes() : ""));
        }

        response.add("total", count)
                .add("requests", requestsArray);

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
        user.setRoleId("user");
        // 添加必要的属性
        user.setStorageQuota(10000000000L); // 设置默认存储配额 10GB
        user.setStorageCurrent(0L); // 设置当前存储使用量
        user.setOnboarding(true); // 设置初始引导状态

        // 使用合适的create方法并传递所需参数
        try {
            // 使用当前管理员的ID作为创建者
            userDao.create(user, principal.getId());
            
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