package kr.ac.ajou.hnm.sensortracker.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Service;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.ac.ajou.hnm.sensortracker.R;
import kr.ac.ajou.hnm.sensortracker.model.Distance;
import kr.ac.ajou.hnm.sensortracker.model.Record;
import kr.ac.ajou.hnm.sensortracker.service.MonitorService;
import kr.ac.ajou.hnm.sensortracker.ui.activity.MainActivity;

public class MainFragment extends Fragment {

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @BindView(R.id.graph)
    GraphView mGraphView;

    @BindView(R.id.printing_board)
    TextView mPrintingBoard;

    MonitorService mService;

    ArrayList<Record> recordsList;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);

        mService = ((MainActivity)getActivity()).mService;

        initGraph();
        return view;
    }

    private void initGraph() {
        mGraphView.getViewport().setScalable(false);
        mGraphView.getViewport().setScalableY(false);
        mGraphView.getViewport().setScrollable(false);
        mGraphView.getViewport().setScrollableY(false);

        mGraphView.getViewport().setYAxisBoundsManual(true);
        mGraphView.getViewport().setMaxY(150);
        mGraphView.getViewport().setMinY(-150);

        mGraphView.getViewport().setXAxisBoundsManual(true);
        mGraphView.getViewport().setMaxX(150);
        mGraphView.getViewport().setMinX(-150);

        PointsGraphSeries<DataPoint> pointSeries = new PointsGraphSeries<>();
        pointSeries.appendData(new DataPoint(-20, 50), true, 1000);
        pointSeries.appendData(new DataPoint(80, 50), true, 1000);
        pointSeries.setSize(9f);

        mGraphView.addSeries(pointSeries);

    }

    void printBoard(String line){
        mPrintingBoard.append(line + "\n");

        // auto scroll for text view
        final int scrollAmount = mPrintingBoard.getLayout().getLineTop(mPrintingBoard.getLineCount()) - mPrintingBoard.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            mPrintingBoard.scrollTo(0, scrollAmount);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @OnClick(R.id.main_initialize)
    void onclick_main_initialize(){

    }

    @SuppressLint("DefaultLocale")
    @OnClick(R.id.main_get_distances)
    void onclick_main_get_distances(){
        if (mService == null)
            mService = ((MainActivity)getActivity()).mService;
        ArrayList <Distance> distanceVector= mService.getDistancesVector();
        if (distanceVector == null) return;
        for (Distance distance : distanceVector){
            printBoard(String.format("[%06d][%d] : %6d\n", distance.seq, distance.id, distance.distance));
        }
    }
}