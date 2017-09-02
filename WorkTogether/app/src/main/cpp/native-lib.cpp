#include <jni.h>
#include <string>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>

extern "C" {

using namespace std;
using namespace cv;

int srcWidth, srcHeight, width, height;

void processSize(){
    double ratio = ((double)srcWidth / srcHeight);
    if(srcWidth > srcHeight){
        if(srcWidth > width){
            srcWidth = width;
            srcHeight = (int)(width/ratio);
        }
        else{
            srcWidth = srcWidth / 2;
            srcHeight = srcHeight / 2;
        }
    }
    else {
        ratio = ((double)srcHeight / srcWidth);
        if(srcHeight > height){
            srcHeight = height;
            srcWidth = (int)(height / ratio);
        }
        else{
            srcWidth = (int)((double)srcWidth * 0.9);
            srcHeight = (int)((double)srcHeight * 0.9);
        }
    }
}

JNIEXPORT
jstring
JNICALL
Java_jang_worktogether_fileandimage_ImageViewActivity_processBitmap(
        JNIEnv *env,
        jobject jo, jstring path, jstring tmpPath, jint dwidth, jint dheight) {
    const char * imagePath = (env)->GetStringUTFChars(path, NULL);
    const char * imageTmpPath = (env)->GetStringUTFChars(tmpPath, NULL);
    Mat src = imread(imagePath, CV_LOAD_IMAGE_COLOR | CV_LOAD_IMAGE_ANYDEPTH);
    Mat resized;
    if(!src.data){
        std::string failed = "read failed";
        return env->NewStringUTF(failed.c_str());
    }

    srcWidth = src.cols;
    srcHeight = src.rows;
    width = dwidth;
    height = dheight;

    if(srcWidth * srcHeight * 4 <= 5000000){
        //이미지 크기가 작으면 원래 이미지 path 리턴
        return env->NewStringUTF(imagePath);
    }

    //이미지 처리 해야하면 임시 path 리턴
    while(srcWidth * srcHeight * 4 > 5000000){
        processSize();
    }

    resize(src, resized, cv::Size(srcWidth, srcHeight), 0, 0, CV_INTER_AREA);
    src.release();

    imwrite(imageTmpPath, resized);
    resized.release();

    return env->NewStringUTF(imageTmpPath);
}

}
