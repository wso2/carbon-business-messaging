<%@ page import="org.wso2.carbon.andes.stub.admin.types.QueueRolePermission" %>
<%@ page import="java.util.ArrayList" %>
<%
    ArrayList<QueueRolePermission> queueRolePermissions = (ArrayList<QueueRolePermission>) session.getAttribute("queueRolePermissions");
    String role = request.getParameter("role");
    String checked = request.getParameter("checked");
    String action = request.getParameter("action");

    for(QueueRolePermission permission: queueRolePermissions){
        if(permission.getRoleName().equals(role)){
            if(action.equals("consume")){
               permission.setAllowedToConsume(checked.equals("true") ? true : false);
            } else if (action.equals("publish")){
                permission.setAllowedToPublish(checked.equals("true") ? true : false);
            }
        }
    }
%>
