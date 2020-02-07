package com.zl.weilu.saf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static int REQUEST_CODE_FOR_SINGLE_FILE = 10001;
    public static int WRITE_REQUEST_CODE = 10002;
    public static int REQUEST_CODE_FOR_DIR = 10003;

    private final String[] IMAGE_PROJECTION = {
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media._ID };

    private ImageView image;
    private TextView mInfo;

    private Uri mFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = findViewById(R.id.image);
        mInfo = findViewById(R.id.info);
    }

    public void selectSingleImage(View view) {
        //通过系统的文件浏览器选择一个文件
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        //筛选，只显示可以“打开”的结果，如文件(而不是联系人或时区列表)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //过滤只显示图像类型文件
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_FOR_SINGLE_FILE);
    }

    public void createFile(View view) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, System.currentTimeMillis() + ".txt");
        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    public void openFile(View view) {
        if (mFileUri != null) {
            StringBuilder stringBuilder = new StringBuilder();
            InputStream inputStream = null;
            try {
                inputStream = getContentResolver().openInputStream(mFileUri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                if (TextUtils.isEmpty(stringBuilder.toString())) {
                    Toast.makeText(this, "内容为空！", Toast.LENGTH_SHORT).show();
                }
                mInfo.setText(stringBuilder.toString());
            } catch (IOException e) {
                Toast.makeText(this, "读取文件失败！", Toast.LENGTH_SHORT).show();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.fillInStackTrace();
                    }
                }
            }
        } else {
            Toast.makeText(this, "请先创建测试文件！", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateFile(View view) {
        if (mFileUri != null) {
            OutputStream outputStream = null;
            try {
                // 获取 OutputStream
                outputStream = getContentResolver().openOutputStream(mFileUri);
                outputStream.write("Storage Access Framework Example".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                Toast.makeText(this, "修改文件失败！", Toast.LENGTH_SHORT).show();
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.fillInStackTrace();
                    }
                }
            }
            Toast.makeText(this, "修改文件内容完成！", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "请先创建测试文件！", Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteFile(View view) {
        if (mFileUri != null) {
            try {
                DocumentsContract.deleteDocument(getContentResolver(), mFileUri);
                mFileUri = null;
            } catch (FileNotFoundException e) {
                Toast.makeText(this, "删除文件失败！", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "请先创建测试文件！", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                //获取Uri
                Uri uri = resultData.getData();
                if (requestCode == REQUEST_CODE_FOR_SINGLE_FILE) {
                    getImageMetaData(uri);
                    showImage(uri);
                } else if (requestCode == WRITE_REQUEST_CODE) {
                    mFileUri = uri;
                    Toast.makeText(this, "文件创建完成，请在文件管理器自行查看。", Toast.LENGTH_SHORT).show();
                } else if (requestCode == REQUEST_CODE_FOR_DIR) {
                    if (uri != null) {
                        // 保存获取的目录权限
                        final int takeFlags = resultData.getFlags()
                                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);

                        // 保存uri
                        SharedPreferences sp = getSharedPreferences("DirPermission", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("uriTree", uri.toString());
                        editor.apply();

                        getDirInfo(uri);
                    }
                }
            }
        }
    }

    public void getDirPermission(View view) {
        SharedPreferences sp = getSharedPreferences("DirPermission", Context.MODE_PRIVATE);
        String uriTree = sp.getString("uriTree", "");
        if (TextUtils.isEmpty(uriTree)) {
            startSafForDirPermission();
        } else {
            try {
                Uri uri = Uri.parse(uriTree);
                final int takeFlags = getIntent().getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, takeFlags);

                getDirInfo(uri);

            } catch (SecurityException e) {
                startSafForDirPermission();
            }
        }
    }

    private void getDirInfo(Uri uri) {
        // 创建所选目录的DocumentFile，可以使用它进行文件操作
        DocumentFile root = DocumentFile.fromTreeUri(this, uri);
        mInfo.setText("Uri: " + root.getUri() + "\n" +
                "Name: " + root.getName());
    }

    private void startSafForDirPermission() {
        // 用户可以选择任意文件夹，将它及其子文件夹的读写权限授予APP。
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_FOR_DIR);
    }

    private void showImage(Uri uri) {
        image.setImageURI(uri);
    }

    public void getImageMetaData(Uri uri) {
        Cursor cursor = this.getContentResolver()
                .query(uri, IMAGE_PROJECTION, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String displayName = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
            String size = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));
            String id = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[2]));

            mInfo.setText(
                    "Uri: " + uri.toString() + "\n" +
                            "Name: " + displayName  + "\n" +
                            "Size: " + size + "Byte");
        }
        cursor.close();
    }
}
