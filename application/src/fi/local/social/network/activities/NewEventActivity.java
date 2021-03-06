package fi.local.social.network.activities;

import java.sql.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
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

import com.example.android.actionbarcompat.ActionBarActivity;

import fi.local.social.network.R;
import fi.local.social.network.db.Event;
import fi.local.social.network.db.EventImpl;
import fi.local.social.network.db.EventsDataSource;

public class NewEventActivity extends ActionBarActivity {

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
	private final int START_DATE_DIALOG_ID=100;
	private final int END_DATE_DIALOG_ID=101;
	private final int START_TIME_PICKER_ID=200;
	private final int END_TIME_PICKER_ID=201;
	private int startYear = 2012;
	private int startMonth =  5;
	private int startDay = 7;
	private int startHour = 11;
	private int startMinute = 0;
	private int endYear = 2012;
	private int endMonth =  5;
	private int endDay = 7;
	private int endHour = 11;
	private int endMinute = 0;

	Date actDate = new Date(System.currentTimeMillis());

	private TimePickerDialog.OnTimeSetListener timePickerListenerStart=
			new TimePickerDialog.OnTimeSetListener() {

		@Override
		public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
			// TODO Auto-generated method stub
			startHour=selectedHour;
			startMinute=selectedMinute;
			eventStartTimeValue.setText(eventStartTimeValue.getText().toString()+" "+
					String.valueOf(startHour)+":"+String.valueOf(startMinute));

		}
	};

	private TimePickerDialog.OnTimeSetListener timePickerListenerEnd=
			new TimePickerDialog.OnTimeSetListener() {

		@Override
		public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
			// TODO Auto-generated method stub
			endHour=selectedHour;
			endMinute=selectedMinute;
			eventEndTimeValue.setText(eventEndTimeValue.getText().toString()+" "+
					String.valueOf(endHour)+":"+String.valueOf(endMinute));

		}
	};

	private DatePickerDialog.OnDateSetListener datePickerListenerStart=
			new DatePickerDialog.OnDateSetListener() {

		//When dialog is closed, method below gets called
		@Override
		public void onDateSet(DatePicker view, int selectedYear, int selectedMonth,int selectedDay) {
			// TODO Auto-generated method stub
			startYear=selectedYear;
			startMonth=selectedMonth+1;
			startDay=selectedDay;
			eventStartTimeValue.setText(""+(startMonth)+"-"+startDay+"-"+startYear);

		}
	};

	private DatePickerDialog.OnDateSetListener datePickerListenerEnd=
			new DatePickerDialog.OnDateSetListener() {

		//When dialog is closed, method below gets called
		@Override
		public void onDateSet(DatePicker view, int selectedYear, int selectedMonth,
				int selectedDay) {
			// TODO Auto-generated method stub
			endYear=selectedYear;
			endMonth=selectedMonth+1;
			endDay=selectedDay;
			eventEndTimeValue.setText(""+(endMonth)+"-"+endDay+"-"+endYear);

		}
	};
	private EventsDataSource eventsDataSource1;
	private GregorianCalendar calendar;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newevent);

		eventEndTimeValue=(TextView)findViewById(R.id.eventEndTimeValue);
		eventStartTimeValue=(TextView)findViewById(R.id.eventStartTimeValue);
		title=(EditText)findViewById(R.id.newEventTitle);
		content=(EditText)findViewById(R.id.newEventContent);
		
		//Restore data when screen rotation happens
		if(DataToRetain.title!=null)
			title.setText(DataToRetain.title);
		if(DataToRetain.content!=null)
			content.setText(DataToRetain.content);
		if(DataToRetain.startTime!=null)
			eventStartTimeValue.setText(DataToRetain.startTime);
		if(DataToRetain.endTime!=null)
			eventEndTimeValue.setText(DataToRetain.endTime);
		
		
		//image=(ImageView)findViewById(R.id.imgView);
		//chooseImageBtn=(Button)findViewById(R.id.chooseImageBtn);
		sendBtn=(Button)findViewById(R.id.sendBtn);
		selectedImage = null;
		sTitle = title.getEditableText().toString();
		sContent = content.getEditableText().toString();
		sUri = "";

		initializeTimeValues();

		// open db
		eventsDataSource = new EventsDataSource(getApplicationContext());
		

		/*
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
		 */


		Button sendBtn = (Button) findViewById(R.id.sendBtn);

		// open db
		eventsDataSource = new EventsDataSource(getApplicationContext());


		// When a user clicks on the 'Send Event' button, the newly created
		// event would be sent to all nearby devices
		sendBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(
						getApplicationContext(),
						"The message is supposed to be sent to nearby devices.",
						Toast.LENGTH_SHORT).show();


				EditText title = (EditText) findViewById(R.id.newEventTitle);
				String sTitle = title.getEditableText().toString();
				EditText content = (EditText) findViewById(R.id.newEventContent);
				String sContent = content.getEditableText().toString();

				sTitle = title.getEditableText().toString();

				sContent = content.getEditableText().toString();

				if (sTitle.equals("")) {
					Toast.makeText(getApplicationContext(),
							"Please add a title.", Toast.LENGTH_SHORT).show();
				} else if (sContent.equals("")) {
					Toast.makeText(getApplicationContext(),
							"Please add a content description.",
							Toast.LENGTH_SHORT).show();
				} else {
					sendNewEvent(sTitle, sContent);
				}
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
			return new DatePickerDialog(this, datePickerListenerStart, startYear, startMonth, startDay);
		case END_DATE_DIALOG_ID:
			return new DatePickerDialog(this, datePickerListenerEnd, endYear, endMonth, endDay);
		case START_TIME_PICKER_ID:
			return new TimePickerDialog(this, timePickerListenerStart, startHour, startMinute,true);
		case END_TIME_PICKER_ID:
			return new TimePickerDialog(this, timePickerListenerEnd, endHour, endMinute,true);
		}
		return null;
	}


	private void sendNewEvent(String sTitle, String sContent) {
		eventsDataSource.open();
		
		GregorianCalendar evStart = 
				new GregorianCalendar(this.startYear,startMonth,startDay,startHour,startMinute);
		GregorianCalendar evEnd = 
				new GregorianCalendar(endYear, endMonth, endDay, endHour, endMinute);
		
		Event event = new EventImpl(evStart.getTimeInMillis(), 
				evEnd.getTimeInMillis(), sTitle, sContent,
				PeopleActivity.USERNAME, null);
		eventsDataSource.createEntry(event.getDBString());
		
		eventsDataSource.close();

		startActivity(new Intent(getApplicationContext(), EventsActivity.class));
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		initializeTimeValues();
	}

	private void initializeTimeValues() {
		calendar = new GregorianCalendar();
		this.startYear = calendar.get(Calendar.YEAR);
		this.startMonth = calendar.get(Calendar.MONTH);
		
		if(calendar.get(Calendar.AM_PM) == 0)
			startHour = calendar.get(Calendar.HOUR);
		else
			startHour = calendar.get(Calendar.HOUR) + 12;

		startMinute = calendar.get(Calendar.MINUTE);
		
		this.endYear = calendar.get(Calendar.YEAR);
		this.endMonth = calendar.get(Calendar.MONTH);
		
		if(calendar.get(Calendar.AM_PM) == 0)
			endHour = calendar.get(Calendar.HOUR);
		else
			endHour = calendar.get(Calendar.HOUR) + 12;

		endMinute = calendar.get(Calendar.MINUTE);
		
		
	}
	//Save the text when screen rotation happens and restore it upon completion of the rotation.
	@Override
    public Object onRetainNonConfigurationInstance(){
    	DataToRetain.title=title.getText().toString();
    	DataToRetain.content=content.getText().toString();
    	DataToRetain.startTime=eventStartTimeValue.getText().toString();
    	DataToRetain.endTime=eventEndTimeValue.getText().toString();
		return null;
    }
}
