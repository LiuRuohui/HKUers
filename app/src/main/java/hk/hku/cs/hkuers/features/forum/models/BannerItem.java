package hk.hku.cs.hkuers.features.forum.models;

public class BannerItem {
    private String title;
    private int imageResId;

    public BannerItem(String title, int imageResId) {
        this.title = title;
        this.imageResId = imageResId;
    }

    public String getTitle() {
        return title;
    }

    public int getImageResId() {
        return imageResId;
    }
}
