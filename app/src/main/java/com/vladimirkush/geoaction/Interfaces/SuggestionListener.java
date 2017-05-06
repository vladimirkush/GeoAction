package com.vladimirkush.geoaction.Interfaces;


import com.vladimirkush.geoaction.Models.LBAction;

public interface SuggestionListener {
     void onSuggestionClicked(int adapterPosition, LBAction action );
}
