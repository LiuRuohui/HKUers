package hk.hku.cs.hkuers.models;

public class Course {
    private String courseIdFireStore;
    private String courseId;
    private String courseClass;
    private String courseSemester;

    public Course() {

    }

    public Course(String courseIdFireStore, String courseId, String courseClass, String courseSemester) {
        this.courseIdFireStore = courseIdFireStore;
        this.courseId = courseId;
        this.courseClass = courseClass;
        this.courseSemester = courseSemester;
    }

    // Getter & Setter
    public String getCourseIdFireStore() { return courseIdFireStore; }
    public void setCourseIdFireStore(String courseIdFireStore) { this.courseIdFireStore = courseIdFireStore; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getCourseClass() { return courseClass; }
    public void setCourseClass(String courseClass) { this.courseClass = courseClass;}
    public String getCourseSemester() { return courseSemester; }
    public void setCourseSemester(String courseSemester) { this.courseSemester = courseSemester; }

}
