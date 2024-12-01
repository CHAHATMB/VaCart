package com.example.vacart.util

import com.example.vacart.model.Bdd
import com.example.vacart.model.StationList

fun getDistinctClassCode(){

}

fun getBirthOccupancyColor(bdd: Bdd?, stationList: StationList?) : Int{
    // states
    // 1 : fully occupied
    // 2 : partialy occupied
    // 3 : vacant

    if(bdd!!.bsd[0].from == stationList!!.stationFrom && bdd.bsd[0].to == stationList.stationTo){
        return if(bdd.bsd[0].occupancy) 1 else 3
    }
    // addition check if berth is occupied in parts
    var prev_station = ""
    for( split in bdd.bsd ){
        if(!split.occupancy || (prev_station != "" && prev_station != split.from)){
            return 2
        }
        prev_station = split.to
    }
    if(prev_station == bdd.to) return 1
    return 2
}