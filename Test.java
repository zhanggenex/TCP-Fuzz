 import java.util.HashSet;
import java.util.Set;

public class Test {

    private int id;
    private Set<String> methods, branch, statements;
    //mutant= methods

    public Test(int id) {
        this.id = id;
        methods = new HashSet<String>();
    }

    public void addMutant(String m) {
        methods.add(m);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Set<String> getMethods() {
        return methods;
    }

    public void setMethods(Set<String> methods) {
        this.methods = methods;
    }

    public Set<String> getBranch() {
        return branch;
    }

    public void setBranch(Set<String> branch) {
        this.branch = branch;
    }

    public Set<String> getStatements() {
        return statements;
    }

    public void setStatements(Set<String> statements) {
        this.statements = statements;
    }
}
