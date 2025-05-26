package udehnih.report.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data

@Builder
@NoArgsConstructor

@AllArgsConstructor
public class UserInfo {
    private Long id;
    private String email;
    private String name;
    private List<String> roles;

    public boolean hasRole(String role) {
        if (roles == null) {
            return false;
        }
        return roles.stream()
                .anyMatch(r -> r.equalsIgnoreCase(role) || 
                          r.equalsIgnoreCase("ROLE_" + role));
    }

    public boolean isStaff() {

 

       return hasRole("STAFF");
    }

    public boolean isStudent() {

 

       return hasRole("STUDENT");
    }

    public boolean isTutor() {

 

       return hasRole("TUTOR");
    }
}
