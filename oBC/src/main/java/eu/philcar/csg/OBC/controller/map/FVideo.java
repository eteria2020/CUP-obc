package eu.philcar.csg.OBC.controller.map;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.VideoView;

import eu.philcar.csg.OBC.ABase;
import eu.philcar.csg.OBC.R;
import eu.philcar.csg.OBC.controller.FBase;
import eu.philcar.csg.OBC.devices.LowLevelInterface;
import eu.philcar.csg.OBC.helpers.DLog;
import eu.philcar.csg.OBC.service.MessageFactory;

/**
 * Created by Momo on 11/01/2018.
 */

public class FVideo extends FBase implements View.OnClickListener {
//	private ImageButton backIB;
	private VideoView video;
	private DLog dlog = new DLog(this.getClass());

	public static FVideo newInstance() {

//		FVideo fv = new FVideo();
		return new FVideo();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ImageButton backIB;
		View view = inflater.inflate(R.layout.f_video, container, false);
		dlog.d("OnCreareView FVideo");
		backIB = (ImageButton) view.findViewById(R.id.fmenBackIB);
		video = (VideoView) view.findViewById(R.id.video);
		backIB.setOnClickListener(this);
		video.setVideoURI(Uri.parse(Environment.getExternalStorageDirectory() + "/video.mp4"));
		video.requestFocus();
		((ABase) getActivity()).sendMessage(MessageFactory.AudioChannel(LowLevelInterface.AUDIO_SYSTEM, 15));

		video.start();

		return view;
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {

			case R.id.fmenBackIB:

				try {
					((ABase) getActivity()).popTillFragment(FHome.class.getName());
				} catch (Exception e) {
					dlog.d("Exception while popping fragment");
				}

				break;

		}

	}
}
