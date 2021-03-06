package com.pdfmerge.proapp2022.pdfmerge;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.pdfmerge.proapp2022.pdfmerge.Controller.PDFMerger;
import com.pdfmerge.proapp2022.pdfmerge.TextToPDF.TextToPDF;
import com.pdfmerge.proapp2022.pdfmerge.Utility.FileComparator;
import com.pdfmerge.proapp2022.pdfmerge.Utility.ItemTouchHelperClass;
import com.pdfmerge.proapp2022.pdfmerge.Utility.PDFDocument;
import com.pdfmerge.proapp2022.pdfmerge.Utility.RecyclerViewEmptySupport;
import com.pdfmerge.proapp2022.pdfmerge.Utility.ViewAnimation;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.theartofdev.edmodo.cropper.CropImage;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MergeActivity extends AppCompatActivity {
    private RecyclerViewEmptySupport mRecyclerView;
    private FloatingActionButton mAddPDFFAB;
    private FloatingActionButton mergePDFFAB;
    FloatingActionButton maddCameraFAB;
    FloatingActionButton maddFilesFAB;
    FloatingActionButton maddTextFAB;
    FloatingActionButton maddHtmlFAB;
    public ProgressBar progressBar;
    EditText passwordText;
    BottomSheetDialog bottomSheetDialog;

    AppCompatCheckBox securePDF;
    private CoordinatorLayout mCoordLayout;
    private static final int READ_REQUEST_CODE = 42;
    private static final int WEBVIEW_REQUEST_CODE = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private CustomRecyclerViewAdapter adapter;
    public ItemTouchHelper itemTouchHelper;
    ArrayList<PDFDocument> dataset;
    private boolean rotate = false;
    private String mCurrentCameraFile;
    private TextView progressBarPercentage;
    private TextView progressBarCount;
    public static final int RESULT_OK = -1;
    private ActionModeCallback actionModeCallback;
    private ActionMode actionMode;
    MergeActivity mergeActivity = null;
    private MenuItem mainMenuItem;
    private boolean isChecked = false;
    Comparator<PDFDocument> comparator = null;
    private FloatingActionButton maddCloudFilesFAB;
    private View lyt_addFiles;
    private View lyt_textToPDF;
    private View lyt_cameraToPDF;
    private View lyt_htmlToPDF;
    private View back_drop;
    private View lyt_addCloudFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        mergeActivity = this;
        //Getting app permission
        CheckStoragePermission();
        //Initiating progress sheet
        InitBottomSheetProgress();

        //Initiating fab buttons
        InitFabButtons();
        //Initiating Recycle view
        InitRecycleViewer();


        Intent intent = getIntent();
        String message = intent.getStringExtra("ActivityAction");
        if (message.equals("FileSearch")) {
            performFileSearch();
        } else if (message.equals("CameraActivity")) {
            StartCameraActivity();
        } else if (message.equals("CloudFileSearch")) {
            performCloudFileSearch();
        }  else if (message.equals("TextToPDFActivity")) {
            StartTextActivity();
        } else if (message.equals("ImageSend")) {
            ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra("image");
            if (imageUris != null) {
                for (int i = 0; i < imageUris.size(); i++) {
                    Uri imageUri = imageUris.get(i);
                    PDFDocument document = new PDFDocument(this, imageUri);
                    addToDataStore(document);
                }
            }
        } else if (message.equals("TextToPDF")) {
            String sharedText = intent.getStringExtra("text");
            Intent textToPDF = new Intent(this, TextToPDF.class);
            textToPDF.putExtra("text", sharedText);
            startActivityForResult(intent, WEBVIEW_REQUEST_CODE);
        }
        actionModeCallback = new ActionModeCallback();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        try {
            if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                if (result != null) {
                    if (result.getClipData() != null) {
                        int count = result.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = result.getClipData().getItemAt(i).getUri();
                            PDFDocument document = new PDFDocument(this, imageUri);
                            addToDataStore(document);
                        }
                    } else if (result.getData() != null) {
                        Uri imageUri = result.getData();
                        PDFDocument document = new PDFDocument(this, imageUri);
                        addToDataStore(document);
                    }
                }
            }
            if (requestCode == WEBVIEW_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                if (result != null) {
                    String fileName = result.getStringExtra("WebFileName");
                    PDFDocument doc = new PDFDocument(fileName, true);
                    addToDataStore(doc);
                }
            }
            if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
                File file = new File(mCurrentCameraFile);
                if (file != null) {
                    if (file.exists()) {
                        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", new File(mCurrentCameraFile));
                        CropImage.activity(uri)
                                .start(this);
                    }
                } else {
                    Toast.makeText(MergeActivity.this, "Unknown error occurs when capture image", Toast.LENGTH_LONG).show();
                }
            }
            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                CropImage.ActivityResult cropped = CropImage.getActivityResult(result);
                if (resultCode == RESULT_OK) {
                    Uri data = cropped.getUri();
                    PDFDocument document = new PDFDocument(this, data);
                    addToDataStore(document);
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = cropped.getError();
                    Toast.makeText(MergeActivity.this, "Unknown error occurs when crop image", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onDestroy() {
        dismissProgressDialog();
        super.onDestroy();
        if (dataset.size() > 0) {
            for (int i = 0; i < dataset.size(); i++) {
                dataset.get(i).deleteFile();
            }
        }
    }

    //>>>>>>>>>Menu for activity>>>>>
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        mainMenuItem = menu.findItem(R.id.fileSort);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.nameSort:
                mainMenuItem.setTitle("Name");
                comparator = FileComparator.getNameFileComparator();
                FileComparator.isDescending = isChecked;
                sortFiles(comparator);
                return true;
            case R.id.modifiedSort:
                mainMenuItem.setTitle("Modified");
                comparator = FileComparator.getLastModifiedFileComparator();
                FileComparator.isDescending = isChecked;
                sortFiles(comparator);
                return true;
            case R.id.sizeSort:
                mainMenuItem.setTitle("Size");
                comparator = FileComparator.getSizeFileComparator();
                FileComparator.isDescending = isChecked;
                sortFiles(comparator);
                return true;
            case R.id.ordering:
                isChecked = !isChecked;
                if (isChecked) {
                    item.setIcon(R.drawable.ic_keyboard_arrow_up_black_24dp);
                } else {
                    item.setIcon(R.drawable.ic_keyboard_arrow_down_black_24dp);
                }
                if (comparator != null) {
                    FileComparator.isDescending = isChecked;
                    sortFiles(comparator);
                } else {
                    comparator = FileComparator.getLastModifiedFileComparator();
                    FileComparator.isDescending = isChecked;
                    sortFiles(comparator);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void sortFiles(Comparator<PDFDocument> comparator) {
        Collections.sort(adapter.mContacts, comparator);
        adapter.notifyDataSetChanged();
    }
    //>>>>>>>>completed>>>>>

    //>>>>>>>>>>>>Main component intialization>>>>>>>>>>
    private void InitBottomSheetProgress() {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet, null);
        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.setCancelable(false);
        progressBar = (ProgressBar) bottomSheetView.findViewById(R.id.progressBar);
        progressBarPercentage = (TextView) bottomSheetView.findViewById(R.id.progressPercentage);
        progressBarCount = (TextView) bottomSheetView.findViewById(R.id.progressCount);
    }

    private void InitFabButtons() {
        maddFilesFAB = (FloatingActionButton) findViewById(R.id.addFilesFAB);
        maddTextFAB = (FloatingActionButton) findViewById(R.id.addTextFAB);
        maddCloudFilesFAB = (FloatingActionButton) findViewById(R.id.addCloudFilesFAB);

        lyt_addFiles = findViewById(R.id.lytm_addFiles);
        lyt_textToPDF = findViewById(R.id.lytm_textToPDF);
        lyt_addCloudFiles = findViewById(R.id.lytm_addCloudFiles);
        back_drop = findViewById(R.id.mback_drop);
        back_drop.setVisibility(View.GONE);
        back_drop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFabMode(mAddPDFFAB);
            }
        });
        ViewAnimation.initShowOut(lyt_addFiles);
        ViewAnimation.initShowOut(lyt_textToPDF);
        ViewAnimation.initShowOut(lyt_addCloudFiles);

        mCoordLayout = (CoordinatorLayout) findViewById(R.id.myCoordinatorLayout);
        mAddPDFFAB = (FloatingActionButton) findViewById(R.id.addToDoItemFAB);
        mAddPDFFAB.setOnClickListener(new View.OnClickListener() {

            @SuppressWarnings("deprecation")
            @Override
            public void onClick(View v) {

                toggleFabMode(v);
            }
        });


        mergePDFFAB = (FloatingActionButton) findViewById(R.id.mergePDFItemFAB);
        mergePDFFAB.setOnClickListener(new View.OnClickListener() {

            @SuppressWarnings("deprecation")
            @Override
            public void onClick(View v) {
                showMergeDialog();
            }

        });



        maddFilesFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFileSearch();
            }

        });

        maddCloudFilesFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performCloudFileSearch();
            }
        });


        maddTextFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartTextActivity();
            }

        });
    }

    private void showMergeDialog() {
        if (dataset.size() < 1) {
            Snackbar.make(mCoordLayout, "You need to add at least 1 file", Snackbar.LENGTH_LONG).show();
        } else {

            final Dialog dialog = new Dialog(mergeActivity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            final View alertView = getLayoutInflater().inflate(R.layout.file_alert_dialog, null);
            LinearLayout layout = (LinearLayout) alertView.findViewById(R.id.savePDFLayout);

            passwordText = (EditText) alertView.findViewById(R.id.password);
            securePDF = (AppCompatCheckBox) alertView.findViewById(R.id.securePDF);
            securePDF.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                    if (b) {
                        passwordText.setVisibility(View.VISIBLE);

                    } else {
                        passwordText.setVisibility(View.GONE);
                    }

                }
            });
            final AppCompatSpinner spn_timezone = (AppCompatSpinner) alertView.findViewById(R.id.compression);

            String[] timezones = new String[]{"Low", "Medium", "High"};
            ArrayAdapter<String> array = new ArrayAdapter<>(getApplicationContext(), R.layout.simple_spinner_item, timezones);
            array.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            spn_timezone.setAdapter(array);
            spn_timezone.setSelection(0);

            final EditText edittext = (EditText) alertView.findViewById(R.id.editText2);
            dialog.setContentView(alertView);
            dialog.setCancelable(true);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.show();
            dialog.getWindow().setAttributes(lp);
            ((ImageButton) dialog.findViewById(R.id.bt_close)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            ((Button) dialog.findViewById(R.id.bt_save)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                            /*if (mInterstitial!=null&&mInterstitial.isReady()) {
                                mInterstitial.show();
                            }*/
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                        CheckStoragePermission();
                    } else {
                        String fileName = edittext.getText().toString();
                        if (!fileName.equals("")) {
                            PDFMerger merger = new PDFMerger(MergeActivity.this, fileName + ".pdf");
                            merger.setDataSet(dataset);
                            merger.setCompression(spn_timezone.getSelectedItem().toString());
                            if (securePDF.isChecked())
                                merger.setPassword(passwordText.getText().toString());
                            merger.execute();
                        } else {
                            Snackbar.make(mCoordLayout, "File name should not be empty", Snackbar.LENGTH_LONG).show();
                        }
                    }
                    dialog.dismiss();
                }
            });
        }
    }

    private void toggleFabMode(View v) {
        rotate = ViewAnimation.rotateFab(v, !rotate);
        if (rotate) {
            ViewAnimation.showIn(lyt_addFiles);
            ViewAnimation.showIn(lyt_textToPDF);
            ViewAnimation.showIn(lyt_addCloudFiles);
            back_drop.setVisibility(View.VISIBLE);
        } else {
            ViewAnimation.showOut(lyt_addFiles);
            ViewAnimation.showOut(lyt_textToPDF);
            ViewAnimation.showOut(lyt_addCloudFiles);
            back_drop.setVisibility(View.GONE);
        }
    }

    private void InitRecycleViewer() {
        mRecyclerView = (RecyclerViewEmptySupport) findViewById(R.id.recycleView);
        mRecyclerView.setEmptyView(findViewById(R.id.toDoEmptyView));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    if (mergePDFFAB.getVisibility() == View.VISIBLE)
                        mergePDFFAB.hide();
                    if (mAddPDFFAB.getVisibility() == View.VISIBLE)
                        mAddPDFFAB.hide();
                    if (rotate) {
                        if (maddHtmlFAB.getVisibility() == View.VISIBLE)
                            maddHtmlFAB.hide();
                        if (maddCameraFAB.getVisibility() == View.VISIBLE)
                            maddCameraFAB.hide();
                        if (maddFilesFAB.getVisibility() == View.VISIBLE)
                            maddFilesFAB.hide();
                        if (maddTextFAB.getVisibility() == View.VISIBLE)
                            maddTextFAB.hide();
                    }
                } else if (dy < 0) {
                    if (mergePDFFAB.getVisibility() != View.VISIBLE)
                        mergePDFFAB.show();
                    if (mAddPDFFAB.getVisibility() != View.VISIBLE)
                        mAddPDFFAB.show();
                    if (rotate) {
                        if (maddHtmlFAB.getVisibility() != View.VISIBLE)
                            maddHtmlFAB.show();
                        if (maddCameraFAB.getVisibility() != View.VISIBLE)
                            maddCameraFAB.show();
                        if (maddFilesFAB.getVisibility() != View.VISIBLE)
                            maddFilesFAB.show();
                        if (maddTextFAB.getVisibility() != View.VISIBLE)
                            maddTextFAB.show();
                    }

                }
            }
        });
        dataset = new ArrayList<PDFDocument>();
        adapter = new CustomRecyclerViewAdapter(dataset);
        adapter.setOnClickListener(new CustomRecyclerViewAdapter.OnClickListener() {
            @Override
            public void onItemClick(View view, PDFDocument obj, int pos) {
                if (adapter.getSelectedItemCount() > 0) {
                    enableActionMode(pos);
                } else {
                    try {
                        // read the inbox which removes bold from the row
                        PDFDocument inbox = adapter.getItem(pos);

                        Intent target = new Intent(Intent.ACTION_VIEW);
                        Uri contentUri = null;
                        if (inbox.getPDFFile() != null) {
                            contentUri = inbox.getPDFFile();
                            if (ContentResolver.SCHEME_FILE.equals(contentUri.getScheme())) {
                                contentUri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", inbox.getFile());

                            }
                        } else {
                            File file = inbox.getFile();
                            contentUri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", file);
                        }
                        target.setData(contentUri);
                        String[] mimetypes = {"image/*", "application/pdf"};
                        target.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        Intent intent = Intent.createChooser(target, "Open File");
                        try {
                            startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Snackbar.make(mCoordLayout, "Install PDF reader application.", Snackbar.LENGTH_LONG).show();
                        }
                    } catch (Exception ex) {
                        Toast.makeText(MergeActivity.this, "Unknown error occurs when opening a file", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onItemLongClick(View view, PDFDocument obj, int pos) {
                enableActionMode(pos);
            }
        });

        adapter.setDragListener(new CustomRecyclerViewAdapter.OnStartDragListener() {
            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                if (actionMode == null)
                    itemTouchHelper.startDrag(viewHolder);
            }
        });
        ItemTouchHelper.Callback callback = new ItemTouchHelperClass(adapter, false);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
        mRecyclerView.setAdapter(adapter);
    }
    //>>>>>>>>completed>>>>>>>>

    //<<<<<<<<<<<Helper Method>>>>>>>>>>>>>
    private void addToDataStore(PDFDocument item) {
        dataset.add(item);
        adapter.notifyItemInserted(dataset.size() - 1);
    }

    private void CheckStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Storage Permission");
                alertDialog.setMessage("Storage permission is required in order to " +
                        "provide PDF merge feature, please enable permission in app settings");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Settings",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                                startActivity(i);
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        2);
            }
        }
    }

    public void performCloudFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/jpeg");
        String[] mimetypes = {"application/pdf", "image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    public void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/jpeg");
        String[] mimetypes = {"application/pdf", "image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(intent, READ_REQUEST_CODE);

    }

    public void StartCameraActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            CheckStoragePermission();
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File root = getCacheDir();
                mCurrentCameraFile = root + "/PDFMerger";
                File myDir = new File(mCurrentCameraFile);
                myDir = new File(mCurrentCameraFile);
                if (!myDir.exists()) {
                    myDir.mkdirs();
                }
                mCurrentCameraFile = root + "/PDFMerger/IMG" + System.currentTimeMillis() + ".jpeg";
                Uri uri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", new File(mCurrentCameraFile));
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }



    public void StartTextActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            CheckStoragePermission();
        } else {
            Intent intent = new Intent(this, TextToPDF.class);
            startActivityForResult(intent, WEBVIEW_REQUEST_CODE);
        }
    }

    public void showBottomSheet(int size) {
        bottomSheetDialog.show();
        this.progressBar.setMax(size);
        this.progressBar.setProgress(0);
    }

    public void setProgress(int progress, int total) {
        this.progressBar.setProgress(progress);
        this.progressBarCount.setText(progress + "/" + total);
        int percentage = (progress * 100) / total;
        this.progressBarPercentage.setText(percentage + "%");
    }

    public void runPostExecution(Boolean isMergeSuccess, final String fileName) {
        dismissProgressDialog();
        makeResult();

    }

    private void dismissProgressDialog() {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            bottomSheetDialog.dismiss();
        }
    }

    public void makeResult() {
        Intent i = new Intent();
        setResult(RESULT_OK, i);
        finish();
    }
    //>>>>>>>Completed>>>>>>>>>>>>>

    //>>>>>>>>>>>>ACtion mode implementation>>>>>>>>>>>>>>>>>
    private void enableActionMode(int position) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback);
        }

        toggleSelection(position);
    }

    private void toggleSelection(int position) {
        adapter.toggleSelection(position);
        ItemTouchHelperClass.isItemSwipe = false;
        int count = adapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    private void selectAll() {
        adapter.selectAll();
        int count = adapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    //Action call back class
    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_delete, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_delete) {
                deleteItems();
                mode.finish();
                return true;
            }
            if (id == R.id.select_all) {
                selectAll();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelections();
            actionMode = null;
            ItemTouchHelperClass.isItemSwipe = true;
        }

        private void deleteItems() {
            List<Integer> selectedItemPositions = adapter.getSelectedItems();
            for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
                adapter.removeData(selectedItemPositions.get(i));
            }
            adapter.notifyDataSetChanged();

        }

    }
}
