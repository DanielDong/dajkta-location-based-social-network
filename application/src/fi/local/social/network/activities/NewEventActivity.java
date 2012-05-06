package fi.local.social.network.activities;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import fi.local.social.network.R;
import fi.local.social.network.db.Event;
import fi.local.social.network.db.EventImpl;
import fi.local.social.network.db.EventsDataSource;

public class NewEventActivity extends Activity {

	private static final int SELECT_PICTURE = 1;
	private TextView eventStartTimeValue,eventEndTimeValue;
	private EditText title=null;
	private EditText content=null;
	private ImageView image=null;
	private Button chooseImageBtn=null;
	private Button sendBtn=null;
	private Bitmap bitmap=null;
	private EventsDataSource eventsDataSource;
	private Uri selectedImage;
	private String sTitle;
	private String sContent;
	private String sUri;
	private long startTime;
	private long endTime;
	private final int START_DATE_DIALOG_ID=100;
	private final int END_DATE_DIALOG_ID=101;
	private final int START_TIME_PICKER_ID=200;
	private final int END_TIME_PICKER_ID=201;
	private int year, month, day, hour, minute;
	
	private TimePickerDialog.OnTimeSetListener timePickerListenerStart=
		new TimePickerDialog.OnTimeSetListener() {
			
			@Override
			public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
				// TODO Auto-generated method stub
				hour=selectedHour;
				minute=selectedMinute;
				eventStartTimeValue.setText(eventStartTimeValue.getText().toString()+" "+
						String.valueOf(hour)+":"+String.valueOf(minute));
				
			}
		};

	private TimePickerDialog.OnTimeSetListener timePickerListenerEnd=
		new TimePickerDialog.OnTimeSetListener() {
			
			@Override
			public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
				// TODO Auto-generated method stub
				hour=selectedHour;
				minute=selectedMinute;
				eventEndTimeValue.setText(eventEndTimeValue.getText().toString()+" "+
						String.valueOf(hour)+":"+String.valueOf(minute));
				
			}
		};
			
	private DatePickerDialog.OnDateSetListener datePickerListenerStart=
		new DatePickerDialog.OnDateSetListener() {
		
		//When dialog is closed, method below gets called
		@Override
		public void onDateSet(DatePicker view, int selectedYear, int selectedMonth,
				int selectedDay) {
			// TODO Auto-generated method stub
			year=selectedYear;
			month=selectedMonth;
			day=selectedDay;
			eventStartTimeValue.setText(""+(month+1)+"-"+day+"-"+year);
		
		}
	};

	private DatePickerDialog.OnDateSetListener datePickerListenerEnd=
		new DatePickerDialog.OnDateSetListener() {
		
		//When dialog is closed, method below gets called
		@Override
		public void onDateSet(DatePicker view, int selectedYear, int selectedMonth,
				int selectedDay) {
			// TODO Auto-generated method stub
			year=selectedYear;
			month=selectedMonth;
			day=selectedDay;
			eventEndTimeValue.setText(""+(month+1)+"-"+day+"-"+year);
		
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newevent);
		eventEndTimeValue=(TextView)findViewById(R.id.eventEndTimeValue);
		eventStartTimeValue=(TextView)findViewById(R.id.eventStartTimeValue);
		title=(EditText)findViewById(R.id.newEventTitle);
		content=(EditText)findViewById(R.id.newEventContent);
		image=(ImageView)findViewById(R.id.imgView);
		chooseImageBtn=(Button)findViewById(R.id.chooseImageBtn);
		sendBtn=(Button)findViewById(R.id.sendBtn);
		selectedImage = null;
		sTitle = title.getEditableText().toString();
		sContent = content.getEditableText().toString();
		sUri = "";
		startTime = 0L; // TODO initialize with the default value
		endTime = 0L; // TODO initialize with the default value

		// open db
		eventsDataSource = new EventsDataSource(getApplicationContext());
		eventsDataSource.open();


		Bitmap bitmapTemp=(Bitmap)getLastNonConfigurationInstance();
		if(bitmapTemp!=null){
			image.setImageBitmap(bitmapTemp);
			bitmap=bitmapTemp;
		}

		//When a user clicks on the 'choose a image' button, the user is directed into the media folder to choose a image
		chooseImageBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				//Launch an image choosing activity
				Intent imageChoosingIntent = new Intent(Intent.ACTION_PICK,
						android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(imageChoosingIntent, SELECT_PICTURE);
			}
		});

		//When a user clicks on the 'Send Event' button, the newly created event would be sent to all nearby devices
		sendBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				Toast.makeText(getApplicationContext(),
						"The message is supposed to be sent to nearby devices.",
						Toast.LENGTH_SHORT).show();

				sTitle = title.getEditableText().toString();
				sContent = content.getEditableText().toString();
				if(selectedImage != null)
					sUri = selectedImage.toString();

				if("".equals(sTitle))
				{
					Toast.makeText(getApplicationContext(),	"Please add a title.",
							Toast.LENGTH_SHORT).show();
					return;
				}else if("".equals(sContent))
				{
					Toast.makeText(getApplicationContext(),	"Please add a content description.",
							Toast.LENGTH_SHORT).show();
					return;
				}
				// TODO check start and endtime if they are the default values????
				else if("".equals(sUri))
				{
					AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(NewEventActivity.this);
					myAlertDialog.setTitle("Picture");
					myAlertDialog.setMessage("You want choose a picutre for your Event?");
					myAlertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface arg0, int arg1) {
							// do nothing and return, so that the user can add the pic
						}});
					myAlertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface arg0, int arg1) {
							// store the event in the db and send it to bt
							// TODO add start- and endtime
							sendNewEvent();
						}});
					myAlertDialog.show();
				}
				sendNewEvent();
			}

		});
	}
	
	//When a user clicks on the label "date" besides "Start from", date dialog is initiated
	public void chooseStartTime(View v){
		showDialog(START_DATE_DIALOG_ID);
	}
	//When a user clicks on the label "date" besides "Last Until:", date dialog is initiated
	public void chooseEndTime(View v){
		showDialog(END_DATE_DIALOG_ID);
	}
	//When a user clicks on the label "time" besides "Start from", date dialog is initiated
	public void chooseStartTimeHour(View v){
		showDialog(START_TIME_PICKER_ID);
	}
	//When a user clicks on the label "time" besides "Last Until:", time picker dialog is initiated
	public void chooseEndTimeHour(View v){
		showDialog(END_TIME_PICKER_ID);
	}
	@Override
	protected Dialog onCreateDialog(int id){
		switch(id){
		case START_DATE_DIALOG_ID:
			return new DatePickerDialog(this, datePickerListenerStart, year, month, day);
		case END_DATE_DIALOG_ID:
			return new DatePickerDialog(this, datePickerListenerEnd, year, month, day);
		case START_TIME_PICKER_ID:
			return new TimePickerDialog(this, timePickerListenerStart, hour, minute,true);
		case END_TIME_PICKER_ID:
			return new TimePickerDialog(this, timePickerListenerEnd, hour, minute,true);
		}
		return null;
	}
	
	private void sendNewEvent() 
	{
		Event event = new EventImpl(startTime, endTime, sTitle, sContent, PeopleActivity.USERNAME, sUri);
		eventsDataSource.createEntry(event.getDBString());
		
		startActivity(new Intent(getApplicationContext(), EventsActivity.class));
	}
	@Override
	protected void onPause() {
		super.onPause();
		eventsDataSource.close();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		eventsDataSource.close();
	}
	@Override
	protected void onResume() {
		super.onResume();
		eventsDataSource.open();
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

		switch(requestCode) { 
		case SELECT_PICTURE:
			if(resultCode == RESULT_OK){  
				//Uri of the selected image by user
				selectedImage = imageReturnedIntent.getData();
				InputStream imageStream=null;
				try {
					imageStream = getContentResolver().openInputStream(selectedImage);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				//Get bitmap format of the selected image
				Bitmap yourSelectedImage = BitmapFactory.decodeStream(imageStream);
				//Show the image in the ImageView
				image.setImageBitmap(yourSelectedImage);
				bitmap=yourSelectedImage;//Store the bitmap of image in case of screen rotation
			}
		}
	}

	//Save the bitmap representation of the image when screen rotates and restore it upon completion of rotation.
	@Override
	public Object onRetainNonConfigurationInstance(){
		return bitmap;
	}
}
