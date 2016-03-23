package io.github.alsoltani.wavestagram.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import boofcv.abst.denoise.FactoryImageDenoise;
import boofcv.abst.denoise.WaveletDenoiseFilter;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.android.ConvertBitmap;

import com.commonsware.cwac.camera.CameraHost;
import com.commonsware.cwac.camera.CameraHostProvider;
import com.commonsware.cwac.camera.CameraView;
import com.commonsware.cwac.camera.PictureTransaction;
import com.commonsware.cwac.camera.SimpleCameraHost;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import boofcv.android.ImplConvertBitmap;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import butterknife.Bind;
import butterknife.OnClick;
import io.github.alsoltani.wavestagram.R;
import io.github.alsoltani.wavestagram.ui.utils.Utils;
import io.github.alsoltani.wavestagram.ui.adapter.PhotoFiltersAdapter;
import io.github.alsoltani.wavestagram.ui.view.RevealBackgroundView;

public class TakePhotoActivity extends BaseActivity implements RevealBackgroundView.OnStateChangeListener,
        CameraHostProvider {
    public static final String ARG_REVEAL_START_LOCATION = "reveal_start_location";

    private static final Interpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final Interpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final int STATE_TAKE_PHOTO = 0;
    private static final int STATE_SETUP_PHOTO = 1;
    private static final int OUTPUT_ORIGINAL = 0;
    private static final int OUTPUT_NOISY = 1;
    private static final int OUTPUT_DENOISED = 2;

    @Bind(R.id.vRevealBackground)
    RevealBackgroundView vRevealBackground;
    @Bind(R.id.vPhotoRoot)
    View vTakePhotoRoot;
    @Bind(R.id.vShutter)
    View vShutter;
    @Bind(R.id.ivTakenPhoto)
    ImageView ivTakenPhoto;
    @Bind(R.id.vUpperPanel)
    ViewSwitcher vUpperPanel;
    @Bind(R.id.vLowerPanel)
    ViewSwitcher vLowerPanel;
    @Bind(R.id.cameraView)
    CameraView cameraView;
    @Bind(R.id.rvFilters)
    RecyclerView rvFilters;
    @Bind(R.id.btnTakePhoto)
    Button btnTakePhoto;
    @Bind(R.id.btnClose)
    ImageView btnClose;

    CoordinatorLayout clContent;

    private boolean pendingIntro;
    public boolean isGaussian = false;
    public boolean isDenoised = false;
    public int currentState;

    private File originalPath;
    private File noisyPath;
    private File denoisedPath;

    public Bitmap originalBitmap;
    public Bitmap noisyBitmap = originalBitmap;

    public static void startCameraFromLocation(int[] startingLocation, Activity startingActivity) {
        Intent intent = new Intent(startingActivity, TakePhotoActivity.class);
        intent.putExtra(ARG_REVEAL_START_LOCATION, startingLocation);
        startingActivity.startActivity(intent);
    }

    public static File getNoisyPath(File originalPath) {
        String pathName = originalPath.getName();
        int i = pathName.contains(".") ? pathName.lastIndexOf('.') : pathName.length();
        String destName = pathName.substring(0, i) + "_NOISY" + pathName.substring(i);
        return (new File(MainActivity.galleryPath, destName));
    }

    public static File getDenoisedPath(File originalPath) {
        String pathName = originalPath.getName();
        int i = pathName.contains(".") ? pathName.lastIndexOf('.') : pathName.length();
        String destName = pathName.substring(0, i) + "_DENOISED" + pathName.substring(i);
        return (new File(MainActivity.galleryPath, destName));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        updateStatusBarColor();
        updateState(STATE_TAKE_PHOTO);
        setupRevealBackground(savedInstanceState);
        setupPhotoFilters();

        vUpperPanel.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                vUpperPanel.getViewTreeObserver().removeOnPreDrawListener(this);
                pendingIntro = true;
                vUpperPanel.setTranslationY(-vUpperPanel.getHeight());
                vLowerPanel.setTranslationY(vLowerPanel.getHeight());
                return true;
            }
        });

        // Change picture depending on PhotoFilersAdapter's Recycler View here.
        //switchDenoisedImage();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void updateStatusBarColor() {
        if (Utils.isAndroid5()) {
            getWindow().setStatusBarColor(0xff111111);
        }
    }

    private void setupRevealBackground(Bundle savedInstanceState) {
        vRevealBackground.setFillPaintColor(0xFF16181a);
        vRevealBackground.setOnStateChangeListener(this);
        if (savedInstanceState == null) {
            final int[] startingLocation = getIntent().getIntArrayExtra(ARG_REVEAL_START_LOCATION);
            vRevealBackground.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    vRevealBackground.getViewTreeObserver().removeOnPreDrawListener(this);
                    vRevealBackground.startFromLocation(startingLocation);
                    return true;
                }
            });
        } else {
            vRevealBackground.setToFinishedFrame();
        }
    }

    private void setupPhotoFilters() {

        PhotoFiltersAdapter photoFiltersAdapter = new PhotoFiltersAdapter(this);

        rvFilters.setHasFixedSize(true);
        rvFilters.setAdapter(photoFiltersAdapter);
        rvFilters.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (currentState == STATE_SETUP_PHOTO) {
            if (intent.getStringExtra("isDenoised").equals("false")) {

                MultiSpectral<ImageFloat32> noisyMulti = ConvertBitmap.bitmapToMS(noisyBitmap, null, ImageFloat32.class, null);
                MultiSpectral<ImageFloat32> denoisedMulti = noisyMulti.createSameShape();

                // How many levels in wavelet transform
                int numLevels = 6;

                // Create the noise removal algorithm
                WaveletDenoiseFilter<ImageFloat32> denoiser =
                        FactoryImageDenoise.waveletVisu(ImageFloat32.class, numLevels, 0, 255);

                // Apply Gaussian blur to each band in the image
                for (int i = 0; i < noisyMulti.getNumBands(); i++) {
                    //BlurImageOps.gaussian(noisy.getBand(i), denoised.getBand(i), -1, 5, null);
                    denoiser.process(noisyMulti.getBand(i), denoisedMulti.getBand(i));
                }

                Bitmap denoisedBitmap = noisyBitmap.copy(noisyBitmap.getConfig(), true);
                ImplConvertBitmap.multiToBitmapRGB_F32(denoisedMulti, denoisedBitmap);

                // Show image. Set output state.
                animateShutter();
                ivTakenPhoto.setImageBitmap(denoisedBitmap);
                isDenoised = true;

                //Save image.
                Utils.saveBitmapToPath(denoisedPath, denoisedBitmap);

            } else {
                animateShutter();
                ivTakenPhoto.setImageBitmap(originalBitmap);
                isDenoised = false;
                Utils.saveBitmapToPath(originalPath, originalBitmap);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.onPause();
    }

    @OnClick(R.id.btnTakePhoto)
    public void onTakePhotoClick() {
        btnTakePhoto.setEnabled(false);
        cameraView.takePicture(true, true);
        animateShutter();
    }

    @OnClick(R.id.btnAccept)
    public void onAcceptClick() {
        if (isDenoised) {
            PublishActivity.openWithPhotoUri(this, Uri.fromFile(denoisedPath));

            originalPath.delete();
            noisyPath.delete();

            MainActivity.deleteFileFromMediaStore(getContentResolver(), originalPath);
            MainActivity.deleteFileFromMediaStore(getContentResolver(), noisyPath);

        } else if (isGaussian) {
            PublishActivity.openWithPhotoUri(this, Uri.fromFile(noisyPath));

            originalPath.delete();
            denoisedPath.delete();

            MainActivity.deleteFileFromMediaStore(getContentResolver(), originalPath);
            MainActivity.deleteFileFromMediaStore(getContentResolver(), denoisedPath);

        } else {
            PublishActivity.openWithPhotoUri(this, Uri.fromFile(originalPath));

            noisyPath.delete();
            denoisedPath.delete();

            MainActivity.deleteFileFromMediaStore(getContentResolver(), noisyPath);
            MainActivity.deleteFileFromMediaStore(getContentResolver(), denoisedPath);
        }
    }

    @OnClick(R.id.btnClose)
    public void onClosePressed() {

        int[] startingLocation = new int[2];
        btnClose.getLocationOnScreen(startingLocation);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        startingLocation[0] += btnClose.getWidth() / 2;
        overridePendingTransition(0, 0);
    }

    @OnClick(R.id.btnBack)
    public void onBackPressed() {
        if (currentState == STATE_SETUP_PHOTO) {
            btnTakePhoto.setEnabled(true);
            vUpperPanel.showNext();
            vLowerPanel.showNext();

            //If not keeping the picture, delete it.

            List<File> paths = Arrays.asList(originalPath, noisyPath, denoisedPath);

            for (ListIterator<File> iter = paths.listIterator(); iter.hasNext(); ) {

                File path = iter.next();
                String fileName = path.getName();
                Log.v("ShowDeletePath", fileName);
                path.delete();

                MainActivity.deleteFileFromMediaStore(getContentResolver(), new File(MainActivity.galleryPath + fileName));
            }

            updateState(STATE_TAKE_PHOTO);

        } else {
            super.onBackPressed();
        }
    }

    @OnClick(R.id.btnGaussian)
    public void onGaussianClick() {

        if (currentState == STATE_SETUP_PHOTO) {
            if (!isGaussian) {

                Random rand = new Random(234);
                MultiSpectral<ImageFloat32> noisyMulti = ConvertBitmap.bitmapToMS(originalBitmap, null, ImageFloat32.class, null);

                // Apply Gaussian blur to each band in the image
                for (int i = 0; i < noisyMulti.getNumBands(); i++) {
                    GImageMiscOps.addGaussian(noisyMulti.getBand(i), rand, 20, 0, 255);
                }

                noisyBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
                ImplConvertBitmap.multiToBitmapRGB_F32(noisyMulti, noisyBitmap);

                // Show image. Set output state.
                animateShutter();
                ivTakenPhoto.setImageBitmap(noisyBitmap);
                isGaussian = true;

                //Save image.
                Utils.saveBitmapToPath(noisyPath, noisyBitmap);

            } else {
                noisyBitmap = originalBitmap;
                animateShutter();
                ivTakenPhoto.setImageBitmap(originalBitmap);
                isGaussian = false;

                Utils.saveBitmapToPath(originalPath, originalBitmap);
            }
        }
    }

    private void animateShutter() {
        vShutter.setVisibility(View.VISIBLE);
        vShutter.setAlpha(0.f);

        ObjectAnimator alphaInAnim = ObjectAnimator.ofFloat(vShutter, "alpha", 0f, 0.8f);
        alphaInAnim.setDuration(100);
        alphaInAnim.setStartDelay(100);
        alphaInAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        ObjectAnimator alphaOutAnim = ObjectAnimator.ofFloat(vShutter, "alpha", 0.8f, 0f);
        alphaOutAnim.setDuration(200);
        alphaOutAnim.setInterpolator(DECELERATE_INTERPOLATOR);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(alphaInAnim, alphaOutAnim);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                vShutter.setVisibility(View.GONE);
            }
        });
        animatorSet.start();
    }

    @Override
    public void onStateChange(int state) {
        if (RevealBackgroundView.STATE_FINISHED == state) {
            vTakePhotoRoot.setVisibility(View.VISIBLE);
            if (pendingIntro) {
                startIntroAnimation();
            }
        } else {
            vTakePhotoRoot.setVisibility(View.INVISIBLE);
        }
    }

    private void startIntroAnimation() {
        vUpperPanel.animate().translationY(0).setDuration(400).setInterpolator(DECELERATE_INTERPOLATOR);
        vLowerPanel.animate().translationY(0).setDuration(400).setInterpolator(DECELERATE_INTERPOLATOR).start();
    }

    @Override
    public CameraHost getCameraHost() {
        return new MyCameraHost(this);
    }

    class MyCameraHost extends SimpleCameraHost {

        private Camera.Size previewSize;
        private File photoDirectory;

        //Rewrite all classes using PhotoDirectory,
        //so that MyCameraHost uses the dedicated dir to store images.

        private void initPhotoDirectory() {
            photoDirectory = new File(MainActivity.galleryPath);
        }

        protected File getPhotoPath() {
            File dir = getPhotoDirectory();
            if (!dir.exists()) {
                dir.mkdir();
            }

            return (new File(dir, getPhotoFilename()));
        }

        protected File getPhotoDirectory() {
            if (photoDirectory == null) {
                initPhotoDirectory();
            }

            return (photoDirectory);
        }

        public MyCameraHost(Context ctxt) {
            super(ctxt);
        }

        @Override
        public boolean useFullBleedPreview() {
            return true;
        }

        @Override
        public Camera.Size getPictureSize(PictureTransaction xact, Camera.Parameters parameters) {
            return previewSize;
        }

        @Override
        public Camera.Parameters adjustPreviewParameters(Camera.Parameters parameters) {
            Camera.Parameters parameters1 = super.adjustPreviewParameters(parameters);
            previewSize = parameters1.getPreviewSize();
            return parameters1;
        }

        @Override
        public void saveImage(PictureTransaction xact, final Bitmap bitmap) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showTakenPicture(bitmap);
                }

            });
        }

        @Override
        public void saveImage(PictureTransaction xact, byte[] image) {
            super.saveImage(xact, image);
            originalPath = getPhotoPath();
            noisyPath = getNoisyPath(originalPath);
            denoisedPath = getDenoisedPath(originalPath);

            List<File> paths = Arrays.asList(originalPath, noisyPath, denoisedPath);

            for (ListIterator<File> iter = paths.listIterator(); iter.hasNext(); ) {

                File path = iter.next();
                if (path.exists()) {
                    path.delete();
                }

                try {
                    FileOutputStream fos = new FileOutputStream(path.getPath());
                    BufferedOutputStream bos = new BufferedOutputStream(fos);

                    bos.write(image);
                    bos.flush();
                    fos.getFD().sync();
                    bos.close();
                } catch (java.io.IOException e) {
                    handleException(e);
                }
            }
        }
    }

    private void showTakenPicture(Bitmap bitmap) {
        vUpperPanel.showNext();
        vLowerPanel.showNext();
        originalBitmap = bitmap;
        ivTakenPhoto.setImageBitmap(bitmap);
        updateState(STATE_SETUP_PHOTO);
    }

    private void updateState(int state) {
        currentState = state;
        if (currentState == STATE_TAKE_PHOTO) {
            vUpperPanel.setInAnimation(this, R.anim.slide_in_from_right);
            vLowerPanel.setInAnimation(this, R.anim.slide_in_from_right);
            vUpperPanel.setOutAnimation(this, R.anim.slide_out_to_left);
            vLowerPanel.setOutAnimation(this, R.anim.slide_out_to_left);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ivTakenPhoto.setVisibility(View.GONE);
                }
            }, 400);
        } else if (currentState == STATE_SETUP_PHOTO) {
            vUpperPanel.setInAnimation(this, R.anim.slide_in_from_left);
            vLowerPanel.setInAnimation(this, R.anim.slide_in_from_left);
            vUpperPanel.setOutAnimation(this, R.anim.slide_out_to_right);
            vLowerPanel.setOutAnimation(this, R.anim.slide_out_to_right);
            ivTakenPhoto.setVisibility(View.VISIBLE);
        }
    }
}
