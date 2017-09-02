package jang.worktogether.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.List;


public class FileUtil {

    public static void viewFile(Context ctx, File file) {
        // TODO Auto-generated method stub
        Intent fileLinkIntent = new Intent(Intent.ACTION_VIEW);
        fileLinkIntent.addCategory(Intent.CATEGORY_DEFAULT);
        //확장자 구하기
        String extension = FilenameUtils.getExtension(file.getAbsolutePath());
        // 파일 확장자 별로 mime type 지정해 준다.
        fileLinkIntent.setDataAndType(Uri.fromFile(file),
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
        PackageManager pm = ctx.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(fileLinkIntent,
                PackageManager.GET_META_DATA);
        if (list.size() == 0) {
            Toast.makeText(ctx, file.getName() + "을 확인할 수 있는 앱이 설치되지 않았습니다.",
                    Toast.LENGTH_SHORT).show();
        } else {
            ctx.startActivity(fileLinkIntent);
        }
    }
}
