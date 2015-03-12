<%@ page import="org.wso2.carbon.andes.stub.admin.types.QueueRolePermission" %>
<%@ page import="java.util.ArrayList" %>
<%
    ArrayList<QueueRolePermission> queueRolePermissions = (ArrayList<QueueRolePermission>) session.getAttribute("queueRolePermissions");
    String role = request.getParameter("role");
    String checked = request.getParameter("checked");
    String action = request.getParameter("action");

    for(QueueRolePermission permission: queueRolePermissions){
        if(permission.getRoleName().equals(role)){
            if ("consume".equals(action)){
                permission.setAllowedToConsume(Boolean.valueOf(checked));
            } else if ("publish".equals(action)){
                permission.setAllowedToPublish(Boolean.valueOf(checked));
            }
        }
    }
%>
