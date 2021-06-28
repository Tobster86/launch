package com.apps.fast.launch.launchviews;

import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.apps.fast.launch.R;
import com.apps.fast.launch.UI.LaunchUICommon.AvatarPurpose;
import com.apps.fast.launch.activities.MainActivity;
import com.apps.fast.launch.components.ClientDefs;
import com.apps.fast.launch.views.AvatarEditView;

import java.io.ByteArrayOutputStream;

import launch.game.Defs;
import launch.game.LaunchClientGame;

/**
 * Created by tobster on 20/10/15.
 */
public class UploadAvatarView extends LaunchView
{
    private static final int FULLY_OPAQUE = -1;

    private AvatarEditView imgAvatarMain;
    private TextView txtHelpAvatar;
    private TextView txtPreview;
    private ImageView imgPreview;
    private LinearLayout btnSelectImage;
    private LinearLayout btnTakeImage;
    private LinearLayout btnChangeAvatar;
    private LinearLayout btnCancel;

    private Bitmap avatar = null;
    private AvatarPurpose purpose;

    public UploadAvatarView(LaunchClientGame game, MainActivity activity, AvatarPurpose purpose)
    {
        super(game, activity);
        this.purpose = purpose;
    }

    @Override
    protected void Setup()
    {
        inflate(context, R.layout.view_upload_avatar, this);

        imgAvatarMain = findViewById(R.id.imgAvatarMain);
        txtHelpAvatar = findViewById(R.id.txtHelpAvatar);
        txtPreview = findViewById(R.id.txtPreview);
        imgPreview = findViewById(R.id.imgPreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnTakeImage = findViewById(R.id.btnTakeImage);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnCancel = findViewById(R.id.btnCancel);

        imgPreview.setMinimumWidth(Defs.AVATAR_SIZE);
        imgPreview.setMinimumHeight(Defs.AVATAR_SIZE);
        imgPreview.setMaxWidth(Defs.AVATAR_SIZE);
        imgPreview.setMaxHeight(Defs.AVATAR_SIZE);

        txtHelpAvatar.setVisibility(GONE);
        txtPreview.setVisibility(GONE);
        btnChangeAvatar.setVisibility(View.GONE);

        btnSelectImage.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //Select image activity for avatar view.
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                activity.startActivityForResult(Intent.createChooser(intent, "Select File"), ClientDefs.ACTIVITY_REQUEST_CODE_AVATAR_IMAGE);
            }
        });

        btnTakeImage.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //Select image activity for avatar view.
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                activity.startActivityForResult(intent, ClientDefs.ACTIVITY_REQUEST_CODE_AVATAR_CAMERA);
            }
        });

        btnChangeAvatar.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(avatar != null)
                {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    avatar.compress(Bitmap.CompressFormat.PNG, 0, baos);

                    byte[] cResult = baos.toByteArray();

                    game.UploadAvatar(cResult);
                }
                else
                {
                    activity.ShowBasicOKDialog(context.getString(R.string.null_avatar));
                }
            }
        });

        btnCancel.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                activity.ReturnToMainView();
            }
        });
    }

    @Override
    public void Update()
    {

    }

    public void ImageActivityResult(Intent data)
    {
        Uri uri = data.getData();

        CursorLoader cursorLoader = new CursorLoader(context, uri, new String[] { MediaStore.MediaColumns.DATA }, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();
        cursor.moveToFirst();

        String selectedImagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));

        BitmapFactory.Options options = new BitmapFactory.Options();

        Bitmap bitmap = BitmapFactory.decodeFile(selectedImagePath, options);

        if(bitmap != null)
        {
            //Convert to transparency-enabled.
            Bitmap transparencyEnabledBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(transparencyEnabledBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            canvas.drawBitmap(bitmap, 0, 0, paint);

            imgAvatarMain.SetAvatar(transparencyEnabledBitmap, this, purpose);

            btnChangeAvatar.setVisibility(View.VISIBLE);
            txtPreview.setVisibility(View.VISIBLE);
            txtHelpAvatar.setVisibility(View.VISIBLE);
        }
    }

    public void CameraActivityResult(Intent data)
    {
        Bitmap photo = (Bitmap) data.getExtras().get("data");

        if(photo != null)
        {
            imgAvatarMain.SetAvatar(photo, this, purpose);

            btnChangeAvatar.setVisibility(View.VISIBLE);
            txtPreview.setVisibility(View.VISIBLE);
            txtHelpAvatar.setVisibility(View.VISIBLE);
        }
    }

    public void AvatarPreviewReady()
    {
        avatar = imgAvatarMain.GetBitmap();

        switch(purpose)
        {
            case PLAYER:
            {
                //Make circular with space for allegiance ring.
                int lAvatarCentre = Defs.AVATAR_SIZE / 2;
                double dblAvatarExtent = Defs.AVATAR_IMAGE / 2.0;

                for (int x = 0; x < Defs.AVATAR_SIZE; x++)
                {
                    for (int y = 0; y < Defs.AVATAR_SIZE; y++)
                    {
                        double dblDistFromCentre = Math.sqrt(((x - lAvatarCentre) * (x - lAvatarCentre)) + ((y - lAvatarCentre) * (y - lAvatarCentre)));

                        if (dblDistFromCentre > dblAvatarExtent)
                        {
                            avatar.setPixel(x, y, 0x00000000);
                        }
                    }
                }
            }
            break;

            case ALLIANCE:
            {
                //Make space for allegiance rectangle.
                int lNearEdge = (Defs.AVATAR_SIZE - Defs.AVATAR_IMAGE) / 2;
                int lFarEdge = Defs.AVATAR_SIZE - lNearEdge;

                for (int x = 0; x < Defs.AVATAR_SIZE; x++)
                {
                    for (int y = 0; y < Defs.AVATAR_SIZE; y++)
                    {
                        if(x < lNearEdge || x > lFarEdge || y < lNearEdge || y > lFarEdge)
                        {
                            avatar.setPixel(x, y, 0x00000000);
                        }
                    }
                }
            }
            break;
        }

        imgPreview.setImageBitmap(avatar);
    }

    public Bitmap GetAvatar() { return avatar; }
}
