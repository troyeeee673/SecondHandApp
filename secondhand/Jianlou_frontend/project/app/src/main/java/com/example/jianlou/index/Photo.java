package com.example.jianlou.index;
import android.net.Uri;

public class Photo {
    private Uri photoID; // 图片Uri

    public Photo(Uri photoImageID) {
        photoID = photoImageID;
    }

    // 修复：规范方法名（可选，若不改则Adapter无需动）
    public Uri getPhotoID() {
        return photoID;
    }

    // 保留原方法（兼容Adapter现有调用）
    public Uri getPhotoPhotoID() {
        return photoID;
    }
}