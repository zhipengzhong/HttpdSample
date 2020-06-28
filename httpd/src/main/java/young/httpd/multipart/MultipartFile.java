package young.httpd.multipart;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MultipartFile {

    private String mName;
    private String mOriginalFilename;
    private String mContentType;
    private long mSize;
    private File mFile;
    private boolean isEmpty;

    public MultipartFile(File file, String originalFilename, String contentType) {
        if (!file.exists()) {
            isEmpty = true;
        }
        mFile = file;
        mName = file.getName();
        mSize = file.length();
        mOriginalFilename = originalFilename;
        mContentType = contentType;
    }

    public File getFile() {
        return mFile;
    }

    public void setFile(File file) {
        mFile = file;
    }

    public String getName() {
        return mName;
    }

    public String getOriginalFilename() {
        return mOriginalFilename;
    }

    public String getContentType() {
        return mContentType;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public long getSize() {
        return mSize;
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(mFile);
    }
}
