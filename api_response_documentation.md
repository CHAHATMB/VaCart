# NTES API Response Documentation & DAO Reference Specification

This document contains verified request specifications and live decrypted JSON responses for all 13 core NTES (National Train Enquiry System) APIs.
All schemas, field names, data types, and values have been captured directly from live NTES production endpoints (`https://enquiry.indianrail.gov.in/crisns/AppServAnd`) and cross-verified against the official NTES Android application decompilation.

> [!NOTE]
> **Transport Layer & Security:**
> All requests are `POST` to `https://enquiry.indianrail.gov.in/crisns/AppServAnd`.
> The request body is `{"jsonIn": "<MD5_HASH>#<HEX_ENCRYPTED_QUERY>"}` and response body is `{"jsonIn": "<HEX_ENCRYPTED_RESPONSE>"}` using AES-128-CBC encryption.
> The JSON samples in this document represent the **decrypted plain JSON payload**, ready for DTO and Room DAO modeling.

## Table of Contents

- [1. Live Train Running Status (Full Route)](#1-live-train-running-status-full-route)
- [2. Station-Specific Spot Train Status](#2-station-specific-spot-train-status)
- [3. Master Train Timetable & Schedule](#3-master-train-timetable-&-schedule)
- [4. Full Date-Specific Schedule](#4-full-date-specific-schedule)
- [5. Active Train Running Instances](#5-active-train-running-instances)
- [6. Station-Level Train Instance Times](#6-station-level-train-instance-times)
- [7. Train Search (By Number / Name)](#7-train-search-by-number--name)
- [8. Station Search (By Code / Name)](#8-station-search-by-code--name)
- [9. Live Station Board (Trains at Station)](#9-live-station-board-trains-at-station)
- [10. Trains Between Two Stations](#10-trains-between-two-stations)
- [11. Station Timetable](#11-station-timetable)
- [12. Average Delay History Statistics](#12-average-delay-history-statistics)
- [13. Train Exception / Alert Info](#13-train-exception--alert-info)

---

## 1. Live Train Running Status (Full Route)

**Endpoint (`subService`)**: `ShowFullRunJson`  
**Description**: Returns comprehensive live train running status, current delay, last reported location, and station-by-station scheduled vs actual arrival/departure times across the entire train journey route.

### Request Specification

**Plain Query String:**
```text
service=TrainRunningMob&subService=ShowFullRunJson&trainNo=11040&jStation=&startDate=19-Oct-2026
```

| Parameter | Sample Value | Description |
| :--- | :--- | :--- |
| `service` | `TrainRunningMob` | Service identifier (fixed) |
| `subService` | `ShowFullRunJson` | Subservice endpoint name |
| `trainNo` | `11040` | 5-digit Indian Railways train number |
| `jStation` | `` | Optional boarding station code (empty for entire route) |
| `startDate` | `19-Oct-2026` | Journey start date in dd-MMM-yyyy format |

### Response JSON Sample (Decrypted)
```json
{
  "DSTNN": "KOLHAPUR",
  "STNS": [
    {
      "STA": "Source",
      "DF": 0,
      "STD": "08:10 19-Oct",
      "depPWDCoachPosition": "1",
      "reversalNumber": 0,
      "DIST": 0,
      "ETD_UA_FLAG": 1,
      "SC": "G",
      "departureCoachPosition": "ENG-SLRD-GEN-GEN-S7-S6-S5-S4-S3-S2-S1-A1-B4-B3-B2-B1-GEN-GEN-LPR",
      "ISA": false,
      "ETA_UA_FLAG": 0,
      "SHN": "गोंदिया जं",
      "SN": "GONDIA JN",
      "ISD": false,
      "arrivalCoachClass": "ENG-SLRD-GEN-GEN-SL-SL-SL-SL-SL-SL-SL-2A-3A-3A-3A-3A-GEN-GEN-LPR",
      "DARR": "",
      "departureCoachClass": "ENG-SLRD-GEN-GEN-SL-SL-SL-SL-SL-SL-SL-2A-3A-3A-3A-3A-GEN-GEN-LPR",
      "ETA": "Source",
      "arrPWDCoachPosition": "1",
      "ETD": "**UA**",
      "DDEP": "On Time",
      "arrivalCoachPosition": "ENG-SLRD-GEN-GEN-S7-S6-S5-S4-S3-S2-S1-A1-B4-B3-B2-B1-GEN-GEN-LPR",
      "PF": "1",
      "SrWTT": 1,
      "Sr": "1"
    },
    {
      "STA": "08:29 19-Oct",
      "DF": 0,
      "STD": "08:31 19-Oct",
      "depPWDCoachPosition": "1",
      "reversalNumber": 0,
      "DIST": 30,
      "ETD_UA_FLAG": 0,
      "SC": "TRO",
      "departureCoachPosition": "ENG-SLRD-GEN-GEN-S7-S6-S5-S4-S3-S2-S1-A1-B4-B3-B2-B1-GEN-GEN-LPR",
      "ISA": true,
      "ETA_UA_FLAG": 0,
      "SHN": "टिरोरा",
      "SN": "TIRORA",
      "ISD": true,
      "arrivalCoachClass": "ENG-SLRD-GEN-GEN-SL-SL-SL-SL-SL-SL-SL-2A-3A-3A-3A-3A-GEN-GEN-LPR",
      "DARR": "00:10",
      "departureCoachClass": "ENG-SLRD-GEN-GEN-SL-SL-SL-SL-SL-SL-SL-2A-3A-3A-3A-3A-GEN-GEN-LPR",
      "ETA": "08:39 19-Oct",
      "arrPWDCoachPosition": "1",
      "ETD": "08:41 19-Oct",
      "DDEP": "00:10",
      "arrivalCoachPosition": "ENG-SLRD-GEN-GEN-S7-S6-S5-S4-S3-S2-S1-A1-B4-B3-B2-B1-GEN-GEN-LPR",
      "PF": "2",
      "SrWTT": 4,
      "Sr": "2"
    }
  ],
  "STD": "19-Oct-2026",
  "LDSRC": 126,
  "TTLDIST": 1345,
  "SRC": "G",
  "AlertMsg": "",
  "NPSTNSR": 18,
  "LSTNSR": 17,
  "NPSTNN": "NAGPUR JN",
  "LEVNT": "AR",
  "NSTNSR": 18,
  "DSTNHN": "कोल्हापुर",
  "ISPTT": true,
  "CPOS": "Arrived at NETAJI SUBHASH(NITR) at 10:20 19-Oct (Delay: 00:22)",
  "LASTUPD": "Arrived at Netaji Subhash at 19-Oct-2026 10:20.",
  "TNH": "महारष्थ्र एक्सप्रेस्",
  "LTIME": "19-Oct-2026 10:20",
  "STNSDISP": [],
  "TNM": "MAHARASHTRA EXP",
  "LSTNNH": "नेता जी सुभाष चंद्र बोस इतवारी जं",
  "AlertMsgHindi": "",
  "NSTN": "NGP",
  "NSTNN": "NAGPUR JN",
  "LUPDT": "10:22 19-Oct",
  "LSTN": "NITR",
  "LUPDFULL": "Arrived at Netaji Subhash at 19-Oct-2026 10:20. Next stoppage station is Nagpur Jn.",
  "SchType": "PTT",
  "STNSD": [],
  "NPSTNNH": "नागपुर जं",
  "DSTN": "KOP",
  "LSTNRT": true,
  "NSTNNH": "नागपुर जं",
  "SRCN": "GONDIA JN",
  "coachPositionFlag": true,
  "LSTNN": "NETAJI SUBHASH",
  "TN": "11040",
  "TRUNST": 1,
  "showCoachPositionFlag": true,
  "NPSTN": "NGP",
  "isArrDSTN": false,
  "LDEL": "22",
  "SRCHN": "गोंदिया जं"
}
```

### Field Dictionary for DAO / DTO Modeling

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `DSTNN` | `String` | Text value / formatted string |
| `STNS` | `List<Object>` | Array of nested objects (see nested schema below) |
| `STD` | `String` | Text value / formatted string |
| `LDSRC` | `Int` | Integer value |
| `TTLDIST` | `Int` | Integer value |
| `SRC` | `String` | Text value / formatted string |
| `AlertMsg` | `String` | Text value / formatted string |
| `NPSTNSR` | `Int` | Integer value |
| `LSTNSR` | `Int` | Integer value |
| `NPSTNN` | `String` | Text value / formatted string |
| `LEVNT` | `String` | Text value / formatted string |
| `NSTNSR` | `Int` | Integer value |
| `DSTNHN` | `String` | Text value / formatted string |
| `ISPTT` | `Boolean` | Boolean flag |
| `CPOS` | `String` | Text value / formatted string |
| `LASTUPD` | `String` | Text value / formatted string |
| `TNH` | `String` | Text value / formatted string |
| `LTIME` | `String` | Text value / formatted string |
| `STNSDISP` | `List<Any>` | Array of elements |
| `TNM` | `String` | Text value / formatted string |
| `LSTNNH` | `String` | Text value / formatted string |
| `AlertMsgHindi` | `String` | Text value / formatted string |
| `NSTN` | `String` | Text value / formatted string |
| `NSTNN` | `String` | Text value / formatted string |
| `LUPDT` | `String` | Text value / formatted string |
| `LSTN` | `String` | Text value / formatted string |
| `LUPDFULL` | `String` | Text value / formatted string |
| `SchType` | `String` | Text value / formatted string |
| `STNSD` | `List<Any>` | Array of elements |
| `NPSTNNH` | `String` | Text value / formatted string |
| `DSTN` | `String` | Text value / formatted string |
| `LSTNRT` | `Boolean` | Boolean flag |
| `NSTNNH` | `String` | Text value / formatted string |
| `SRCN` | `String` | Text value / formatted string |
| `coachPositionFlag` | `Boolean` | Boolean flag |
| `LSTNN` | `String` | Text value / formatted string |
| `TN` | `String` | Text value / formatted string |
| `TRUNST` | `Int` | Integer value |
| `showCoachPositionFlag` | `Boolean` | Boolean flag |
| `NPSTN` | `String` | Text value / formatted string |
| `isArrDSTN` | `Boolean` | Boolean flag |
| `LDEL` | `String` | Text value / formatted string |
| `SRCHN` | `String` | Text value / formatted string |

#### Nested Items in `STNS`

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `STA` | `String` | Sample: `Source` |
| `DF` | `Int` | Sample: `0` |
| `STD` | `String` | Sample: `08:10 19-Oct` |
| `depPWDCoachPosition` | `String` | Sample: `1` |
| `reversalNumber` | `Int` | Sample: `0` |
| `DIST` | `Int` | Sample: `0` |
| `ETD_UA_FLAG` | `Int` | Sample: `1` |
| `SC` | `String` | Sample: `G` |
| `departureCoachPosition` | `String` | Sample: `ENG-SLRD-GEN-GEN-S7-S6-S5-S4-S3-S2-S1-A1` |
| `ISA` | `Boolean` | Sample: `False` |
| `ETA_UA_FLAG` | `Int` | Sample: `0` |
| `SHN` | `String` | Sample: `गोंदिया जं` |
| `SN` | `String` | Sample: `GONDIA JN` |
| `ISD` | `Boolean` | Sample: `False` |
| `arrivalCoachClass` | `String` | Sample: `ENG-SLRD-GEN-GEN-SL-SL-SL-SL-SL-SL-SL-2A` |
| `DARR` | `String` | Sample: `` |
| `departureCoachClass` | `String` | Sample: `ENG-SLRD-GEN-GEN-SL-SL-SL-SL-SL-SL-SL-2A` |
| `ETA` | `String` | Sample: `Source` |
| `arrPWDCoachPosition` | `String` | Sample: `1` |
| `ETD` | `String` | Sample: `**UA**` |
| `DDEP` | `String` | Sample: `On Time` |
| `arrivalCoachPosition` | `String` | Sample: `ENG-SLRD-GEN-GEN-S7-S6-S5-S4-S3-S2-S1-A1` |
| `PF` | `String` | Sample: `1` |
| `SrWTT` | `Int` | Sample: `1` |
| `Sr` | `String` | Sample: `1` |

---

## 2. Station-Specific Spot Train Status

**Endpoint (`subService`)**: `ShowRunStnJS`  
**Description**: Returns train running status relative to a specific scheduled station stop (delay at station, platform, arrival/departure flags, coach composition).

### Request Specification

**Plain Query String:**
```text
service=TrainRunningMob&subService=ShowRunStnJS&trainNo=11040&jStation=G&startDate=19-Oct-2026&stnSr=1
```

| Parameter | Sample Value | Description |
| :--- | :--- | :--- |
| `service` | `TrainRunningMob` | Service identifier (fixed) |
| `subService` | `ShowRunStnJS` | Subservice endpoint name |
| `trainNo` | `11040` | Train number |
| `jStation` | `G` | Station code (e.g. Gondia Junction) |
| `startDate` | `19-Oct-2026` | Journey start date (dd-MMM-yyyy) |
| `stnSr` | `1` | Station serial number in train timetable (1-based) |

### Response JSON Sample (Decrypted)
```json
{
  "DelayArr": "Source",
  "LastEvent": "AR",
  "LastDelay": "00:22",
  "Platform": "1",
  "LastUpdate": "Arrived at NETAJI SUBHASH(NITR) on 10:20 19-Oct (Delay: 00:22) (126 Kms. ahead of GONDIA JN)",
  "LastStationSr": 17,
  "AlertMsg": "",
  "JourneyStationName": "GONDIA JN",
  "NextPTTStationName": "NAGPUR JN",
  "SchArr": "Source",
  "NextPTTStation": "NGP",
  "jsDClassFlag": false,
  "DelayDepMin": "0",
  "vDivertRoute": [],
  "jsArrPWDCoachPosition": "1",
  "StartDate": "19-Oct-2026",
  "vDivertRouteD": [],
  "Notice": "",
  "AlertMsgHindi": "",
  "LastUpdateTime": "19-Oct-2026 10:22",
  "TrainHindiName": "महारष्थ्र एक्सप्रेस्",
  "LastStationHindiName": "नेता जी सुभाष चंद्र बोस इतवारी जं",
  "DestinationName": "KOLHAPUR",
  "jsDepPWDCoachPosition": "1",
  "DistanceToGo": "126",
  "ActArr": "Source",
  "ArrFlag": "false",
  "NextPTTStationHindiName": "नागपुर जं",
  "ActDep": "Update Awaited",
  "coachPositionFlag": true,
  "SchDep": "08:10 19-Oct",
  "DepFlag": "false",
  "trainNumber": "11040",
  "showCoachPositionFlag": true,
  "LastStation": "NITR",
  "LastStationName": "NETAJI SUBHASH",
  "Destination": "KOP",
  "TrainName": "MAHARASHTRA EXP",
  "DestinationHindiName": "कोल्हापुर",
  "NextStation": "NGP",
  "jsTrainReversalFlag": false,
  "SourceHindiName": "गोंदिया जं",
  "Source": "G",
  "JourneyStationHindiName": "गोंदिया जं",
  "Gauge": "",
  "LastStationInRoute": true,
  "DelayDep": "No Delay",
  "DelayArrMin": "Source",
  "LastUpdateFull": "Arrived at NETAJI SUBHASH(NITR) on 10:20 19-Oct (Delay: 00:22) (126 Kms. ahead of GONDIA JN)",
  "JourneyStation": "G",
  "waitDepUpd": "true",
  "SourceName": "GONDIA JN",
  "jsDepartureCoachPosition": "ENG-SLRD-GEN-GEN-S7-S6-S5-S4-S3-S2-S1-A1-B4-B3-B2-B1-GEN-GEN-LPR",
  "jsArrivalCoachPosition": "ENG-SLRD-GEN-GEN-S7-S6-S5-S4-S3-S2-S1-A1-B4-B3-B2-B1-GEN-GEN-LPR",
  "NextStationHindiName": "नागपुर जं",
  "jsDepartureComposition": [
    {
      "coachClass": "ENG",
      "positionFromEngine": "0",
      "coachID": "ENG"
    },
    {
      "coachClass": "SLRD",
      "positionFromEngine": "1",
      "coachID": "SLRD"
    }
  ],
  "jsArrivalComposition": [
    {
      "coachClass": "ENG",
      "positionFromEngine": "0",
      "coachID": "ENG"
    },
    {
      "coachClass": "SLRD",
      "positionFromEngine": "1",
      "coachID": "SLRD"
    }
  ],
  "LastStationTime": "19-Oct-2026 10:20",
  "waitArrUpd": "false",
  "vCancelRoute": [],
  "NextStationName": "NAGPUR JN"
}
```

### Field Dictionary for DAO / DTO Modeling

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `DelayArr` | `String` | Text value / formatted string |
| `LastEvent` | `String` | Text value / formatted string |
| `LastDelay` | `String` | Text value / formatted string |
| `Platform` | `String` | Text value / formatted string |
| `LastUpdate` | `String` | Text value / formatted string |
| `LastStationSr` | `Int` | Integer value |
| `AlertMsg` | `String` | Text value / formatted string |
| `JourneyStationName` | `String` | Text value / formatted string |
| `NextPTTStationName` | `String` | Text value / formatted string |
| `SchArr` | `String` | Text value / formatted string |
| `NextPTTStation` | `String` | Text value / formatted string |
| `jsDClassFlag` | `Boolean` | Boolean flag |
| `DelayDepMin` | `String` | Text value / formatted string |
| `vDivertRoute` | `List<Any>` | Array of elements |
| `jsArrPWDCoachPosition` | `String` | Text value / formatted string |
| `StartDate` | `String` | Text value / formatted string |
| `vDivertRouteD` | `List<Any>` | Array of elements |
| `Notice` | `String` | Text value / formatted string |
| `AlertMsgHindi` | `String` | Text value / formatted string |
| `LastUpdateTime` | `String` | Text value / formatted string |
| `TrainHindiName` | `String` | Text value / formatted string |
| `LastStationHindiName` | `String` | Text value / formatted string |
| `DestinationName` | `String` | Text value / formatted string |
| `jsDepPWDCoachPosition` | `String` | Text value / formatted string |
| `DistanceToGo` | `String` | Text value / formatted string |
| `ActArr` | `String` | Text value / formatted string |
| `ArrFlag` | `String` | Text value / formatted string |
| `NextPTTStationHindiName` | `String` | Text value / formatted string |
| `ActDep` | `String` | Text value / formatted string |
| `coachPositionFlag` | `Boolean` | Boolean flag |
| `SchDep` | `String` | Text value / formatted string |
| `DepFlag` | `String` | Text value / formatted string |
| `trainNumber` | `String` | Text value / formatted string |
| `showCoachPositionFlag` | `Boolean` | Boolean flag |
| `LastStation` | `String` | Text value / formatted string |
| `LastStationName` | `String` | Text value / formatted string |
| `Destination` | `String` | Text value / formatted string |
| `TrainName` | `String` | Text value / formatted string |
| `DestinationHindiName` | `String` | Text value / formatted string |
| `NextStation` | `String` | Text value / formatted string |
| `jsTrainReversalFlag` | `Boolean` | Boolean flag |
| `SourceHindiName` | `String` | Text value / formatted string |
| `Source` | `String` | Text value / formatted string |
| `JourneyStationHindiName` | `String` | Text value / formatted string |
| `Gauge` | `String` | Text value / formatted string |
| `LastStationInRoute` | `Boolean` | Boolean flag |
| `DelayDep` | `String` | Text value / formatted string |
| `DelayArrMin` | `String` | Text value / formatted string |
| `LastUpdateFull` | `String` | Text value / formatted string |
| `JourneyStation` | `String` | Text value / formatted string |
| `waitDepUpd` | `String` | Text value / formatted string |
| `SourceName` | `String` | Text value / formatted string |
| `jsDepartureCoachPosition` | `String` | Text value / formatted string |
| `jsArrivalCoachPosition` | `String` | Text value / formatted string |
| `NextStationHindiName` | `String` | Text value / formatted string |
| `jsDepartureComposition` | `List<Object>` | Array of nested objects (see nested schema below) |
| `jsArrivalComposition` | `List<Object>` | Array of nested objects (see nested schema below) |
| `LastStationTime` | `String` | Text value / formatted string |
| `waitArrUpd` | `String` | Text value / formatted string |
| `vCancelRoute` | `List<Any>` | Array of elements |
| `NextStationName` | `String` | Text value / formatted string |

#### Nested Items in `jsDepartureComposition`

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `coachClass` | `String` | Sample: `ENG` |
| `positionFromEngine` | `String` | Sample: `0` |
| `coachID` | `String` | Sample: `ENG` |

#### Nested Items in `jsArrivalComposition`

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `coachClass` | `String` | Sample: `ENG` |
| `positionFromEngine` | `String` | Sample: `0` |
| `coachID` | `String` | Sample: `ENG` |

---

## 3. Master Train Timetable & Schedule

**Endpoint (`subService`)**: `GetScheduleJson`  
**Description**: Returns master timetable schedule with all halts, scheduled arrival/departure times, distance from source, day offset, and running days.

### Request Specification

**Plain Query String:**
```text
service=TrainRunningMob&subService=GetScheduleJson&trainNo=11040
```

| Parameter | Sample Value | Description |
| :--- | :--- | :--- |
| `service` | `TrainRunningMob` | Service identifier (fixed) |
| `subService` | `GetScheduleJson` | Subservice endpoint name |
| `trainNo` | `11040` | Train number |

### Response JSON Sample (Decrypted)
```json
{
  "Destination": "KOP",
  "TrainNumber": "11040",
  "TrainName": "MAHARASHTRA EXP",
  "TravelClass": "",
  "TrainTypeDescHindi": "मेल एक्सप्रेस",
  "AlertMsg": "",
  "DestinationHindiName": "कोल्हापुर",
  "SourceName": "GONDIA JN",
  "DaysOfRun": "DAILY",
  "stations": [
    {
      "StationName": "GONDIA JN",
      "STA": "",
      "STD": "08:10",
      "Halt": 0,
      "StationHindiName": "गोंदिया जं",
      "DayOfRun": "DAILY",
      "ArrDepFlag": false,
      "Reversal": 0,
      "StationCode": "G",
      "Day": 1,
      "Distance": "0",
      "Sr": 1
    },
    {
      "StationName": "TIRORA",
      "STA": "08:29",
      "STD": "08:31",
      "Halt": 2,
      "StationHindiName": "टिरोरा",
      "DayOfRun": "DAILY",
      "ArrDepFlag": false,
      "Reversal": 0,
      "StationCode": "TRO",
      "Day": 1,
      "Distance": "30",
      "Sr": 2
    }
  ],
  "SourceHindiName": "गोंदिया जं",
  "Source": "G",
  "TravelTime": "27:15",
  "AlertMsgHindi": "",
  "TrainHindiName": "महारष्थ्र एक्सप्रेस्",
  "TrainTypeDesc": "MAIL EXPRESS",
  "DestinationName": "KOLHAPUR",
  "TrainType": "MEX"
}
```

### Field Dictionary for DAO / DTO Modeling

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `Destination` | `String` | Text value / formatted string |
| `TrainNumber` | `String` | Text value / formatted string |
| `TrainName` | `String` | Text value / formatted string |
| `TravelClass` | `String` | Text value / formatted string |
| `TrainTypeDescHindi` | `String` | Text value / formatted string |
| `AlertMsg` | `String` | Text value / formatted string |
| `DestinationHindiName` | `String` | Text value / formatted string |
| `SourceName` | `String` | Text value / formatted string |
| `DaysOfRun` | `String` | Text value / formatted string |
| `stations` | `List<Object>` | Array of nested objects (see nested schema below) |
| `SourceHindiName` | `String` | Text value / formatted string |
| `Source` | `String` | Text value / formatted string |
| `TravelTime` | `String` | Text value / formatted string |
| `AlertMsgHindi` | `String` | Text value / formatted string |
| `TrainHindiName` | `String` | Text value / formatted string |
| `TrainTypeDesc` | `String` | Text value / formatted string |
| `DestinationName` | `String` | Text value / formatted string |
| `TrainType` | `String` | Text value / formatted string |

#### Nested Items in `stations`

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `StationName` | `String` | Sample: `GONDIA JN` |
| `STA` | `String` | Sample: `` |
| `STD` | `String` | Sample: `08:10` |
| `Halt` | `Int` | Sample: `0` |
| `StationHindiName` | `String` | Sample: `गोंदिया जं` |
| `DayOfRun` | `String` | Sample: `DAILY` |
| `ArrDepFlag` | `Boolean` | Sample: `False` |
| `Reversal` | `Int` | Sample: `0` |
| `StationCode` | `String` | Sample: `G` |
| `Day` | `Int` | Sample: `1` |
| `Distance` | `String` | Sample: `0` |
| `Sr` | `Int` | Sample: `1` |

---

## 4. Full Date-Specific Schedule

**Endpoint (`subService`)**: `GetFullScheduleJson`  
**Description**: Returns full sequence of all stations on the route for a specific journey date as formatted tokens (Sr!StationCode!StationName...).

### Request Specification

**Plain Query String:**
```text
service=TrainRunningMob&subService=GetFullScheduleJson&trainNo=11040&startDate=19-Oct-2026
```

| Parameter | Sample Value | Description |
| :--- | :--- | :--- |
| `service` | `TrainRunningMob` | Service identifier (fixed) |
| `subService` | `GetFullScheduleJson` | Subservice endpoint name |
| `trainNo` | `11040` | Train number |
| `startDate` | `19-Oct-2026` | Journey start date (dd-MMM-yyyy) |

### Response JSON Sample (Decrypted)
```json
{
  "AlertMsgHindi": "",
  "AlertMsg": "",
  "stations": [
    "1!G",
    "2!GJ"
  ]
}
```

### Field Dictionary for DAO / DTO Modeling

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `AlertMsgHindi` | `String` | Text value / formatted string |
| `AlertMsg` | `String` | Text value / formatted string |
| `stations` | `List<String>` | Array of string tokens |

---

## 5. Active Train Running Instances

**Endpoint (`subService`)**: `GetTrainInstance`  
**Description**: Returns all active instances of a train running on various dates (e.g. multi-day journeys, yesterday, today, tomorrow) with current live status flags (0=Yet to start, 1=In transit, 2=Completed).

### Request Specification

**Plain Query String:**
```text
service=TrainRunningMob&subService=GetTrainInstance&trainNo=11040
```

| Parameter | Sample Value | Description |
| :--- | :--- | :--- |
| `service` | `TrainRunningMob` | Service identifier (fixed) |
| `subService` | `GetTrainInstance` | Subservice endpoint name |
| `trainNo` | `11040` | Train number |

### Response JSON Sample (Decrypted)
```json
{
  "TrainNameHindi": "महारष्थ्र एक्सप्रेस्",
  "DstnNameHindi": "कोल्हापुर",
  "AlertMsgHindi": "",
  "TrainNo": "11040",
  "TrainName": "MAHARASHTRA EXP",
  "Src": "G",
  "DstnName": "KOLHAPUR",
  "AlertMsg": "",
  "SrcNameHindi": "गोंदिया जं",
  "Dstn": "KOP",
  "SrcName": "GONDIA JN",
  "vInstanceList": [
    {
      "excpMsg": "",
      "trainStatus": 0,
      "trainPosition": "Yet to start from its source",
      "startDate": "20-Oct-2026"
    },
    {
      "excpMsg": "",
      "trainStatus": 1,
      "trainPosition": "Arrived at NETAJI SUBHASH(NITR) at 10:20 19-Oct (Delay: 00:22)",
      "startDate": "19-Oct-2026"
    }
  ]
}
```

### Field Dictionary for DAO / DTO Modeling

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `TrainNameHindi` | `String` | Text value / formatted string |
| `DstnNameHindi` | `String` | Text value / formatted string |
| `AlertMsgHindi` | `String` | Text value / formatted string |
| `TrainNo` | `String` | Text value / formatted string |
| `TrainName` | `String` | Text value / formatted string |
| `Src` | `String` | Text value / formatted string |
| `DstnName` | `String` | Text value / formatted string |
| `AlertMsg` | `String` | Text value / formatted string |
| `SrcNameHindi` | `String` | Text value / formatted string |
| `Dstn` | `String` | Text value / formatted string |
| `SrcName` | `String` | Text value / formatted string |
| `vInstanceList` | `List<Object>` | Array of nested objects (see nested schema below) |

#### Nested Items in `vInstanceList`

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `excpMsg` | `String` | Sample: `` |
| `trainStatus` | `Int` | Sample: `0` |
| `trainPosition` | `String` | Sample: `Yet to start from its source` |
| `startDate` | `String` | Sample: `20-Oct-2026` |

---

## 6. Station-Level Train Instance Times

**Endpoint (`subService`)**: `GetTrainStnInstance`  
**Description**: Returns scheduled arrival and departure timings for the train instances at a designated station.

### Request Specification

**Plain Query String:**
```text
service=TrainRunningMob&subService=GetTrainStnInstance&trainNo=11040&jStation=G
```

| Parameter | Sample Value | Description |
| :--- | :--- | :--- |
| `service` | `TrainRunningMob` | Service identifier (fixed) |
| `subService` | `GetTrainStnInstance` | Subservice endpoint name |
| `trainNo` | `11040` | Train number |
| `jStation` | `G` | Station code |

### Response JSON Sample (Decrypted)
```json
{
  "TrainNameHindi": "महारष्थ्र एक्सप्रेस्",
  "Destination": "KOP",
  "TrainNumber": "11040",
  "AlertMsgHindi": "",
  "TrainName": "MAHARASHTRA EXP",
  "AlertMsg": "",
  "DestinationHindiName": "कोल्हापुर",
  "DestinationName": "KOLHAPUR",
  "SourceName": "GONDIA JN",
  "stations": [
    {
      "STA": "",
      "STD": "08:10 20-Oct",
      "STA_HHMM": "",
      "startDate": "20-Oct-2026",
      "Sr": 1,
      "STD_HHMM": "08:10"
    },
    {
      "STA": "",
      "STD": "08:10 19-Oct",
      "STA_HHMM": "",
      "startDate": "19-Oct-2026",
      "Sr": 1,
      "STD_HHMM": "08:10"
    }
  ],
  "SourceHindiName": "गोंदिया जं",
  "Source": "G"
}
```

### Field Dictionary for DAO / DTO Modeling

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `TrainNameHindi` | `String` | Text value / formatted string |
| `Destination` | `String` | Text value / formatted string |
| `TrainNumber` | `String` | Text value / formatted string |
| `AlertMsgHindi` | `String` | Text value / formatted string |
| `TrainName` | `String` | Text value / formatted string |
| `AlertMsg` | `String` | Text value / formatted string |
| `DestinationHindiName` | `String` | Text value / formatted string |
| `DestinationName` | `String` | Text value / formatted string |
| `SourceName` | `String` | Text value / formatted string |
| `stations` | `List<Object>` | Array of nested objects (see nested schema below) |
| `SourceHindiName` | `String` | Text value / formatted string |
| `Source` | `String` | Text value / formatted string |

#### Nested Items in `stations`

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `STA` | `String` | Sample: `` |
| `STD` | `String` | Sample: `08:10 20-Oct` |
| `STA_HHMM` | `String` | Sample: `` |
| `startDate` | `String` | Sample: `20-Oct-2026` |
| `Sr` | `Int` | Sample: `1` |
| `STD_HHMM` | `String` | Sample: `08:10` |

---

## 7. Train Search (By Number / Name)

**Endpoint (`subService`)**: `FindTrainJson`  
**Description**: Searches for trains matching a given train number or partial name prefix, returning source, destination, train type, and train names.

### Request Specification

**Plain Query String:**
```text
service=TrainRunningMob&subService=FindTrainJson&trainNo=11040
```

| Parameter | Sample Value | Description |
| :--- | :--- | :--- |
| `service` | `TrainRunningMob` | Service identifier (fixed) |
| `subService` | `FindTrainJson` | Subservice endpoint name |
| `trainNo` | `11040` | Train number or search keyword |

### Response JSON Sample (Decrypted)
```json
{
  "Trains": [
    {
      "DestinationHindi": "कोल्हापुर",
      "Destination": "KOP",
      "TrainNumber": "11040",
      "SourceHindi": "गोंदिया जं",
      "Type": "MEX",
      "TrainName": "MAHARASHTRA EXP",
      "TrainHindiName": "महारष्थ्र एक्सप्रेस्",
      "DestinationName": "KOLHAPUR",
      "SourceName": "GONDIA JN",
      "Source": "G"
    }
  ],
  "TrainNoName": "11040",
  "alertMsgHindi": "",
  "alertMsg": ""
}
```

### Field Dictionary for DAO / DTO Modeling

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `Trains` | `List<Object>` | Array of nested objects (see nested schema below) |
| `TrainNoName` | `String` | Text value / formatted string |
| `alertMsgHindi` | `String` | Text value / formatted string |
| `alertMsg` | `String` | Text value / formatted string |

#### Nested Items in `Trains`

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `DestinationHindi` | `String` | Sample: `कोल्हापुर` |
| `Destination` | `String` | Sample: `KOP` |
| `TrainNumber` | `String` | Sample: `11040` |
| `SourceHindi` | `String` | Sample: `गोंदिया जं` |
| `Type` | `String` | Sample: `MEX` |
| `TrainName` | `String` | Sample: `MAHARASHTRA EXP` |
| `TrainHindiName` | `String` | Sample: `महारष्थ्र एक्सप्रेस्` |
| `DestinationName` | `String` | Sample: `KOLHAPUR` |
| `SourceName` | `String` | Sample: `GONDIA JN` |
| `Source` | `String` | Sample: `G` |

---

## 8. Station Search (By Code / Name)

**Endpoint (`subService`)**: `FindStationListJson`  
**Description**: Searches for railway stations by code or station name prefix.

### Request Specification

**Plain Query String:**
```text
service=TrainRunningMob&subService=FindStationListJson&stnStr=DELHI&searchType=N
```

| Parameter | Sample Value | Description |
| :--- | :--- | :--- |
| `service` | `TrainRunningMob` | Service identifier (fixed) |
| `subService` | `FindStationListJson` | Subservice endpoint name |
| `stnStr` | `DELHI` | Search string (station code or name) |
| `searchType` | `N` | Search mode: 'N' for Name, 'C' for Station Code |

### Response JSON Sample (Decrypted)
```json
{
  "AlertMsgHindi": "",
  "AlertMsg": "",
  "recordCount": "13",
  "stations": [
    {
      "C": "DAZ",
      "N": "DELHI AZADPUR"
    },
    {
      "C": "DEC",
      "N": "DELHI CANTT"
    }
  ]
}
```

### Field Dictionary for DAO / DTO Modeling

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `AlertMsgHindi` | `String` | Text value / formatted string |
| `AlertMsg` | `String` | Text value / formatted string |
| `recordCount` | `String` | Text value / formatted string |
| `stations` | `List<Object>` | Array of nested objects (see nested schema below) |

#### Nested Items in `stations`

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `C` | `String` | Sample: `DAZ` |
| `N` | `String` | Sample: `DELHI AZADPUR` |

---

## 9. Live Station Board (Trains at Station)

**Endpoint (`subService`)**: `TrainsAtStationJson`  
**Description**: Returns live train board showing all trains arriving or departing from a given station within a window of hours (e.g. next 2 or 4 hours).

### Request Specification

**Plain Query String:**
```text
service=TrainRunningMob&subService=TrainsAtStationJson&jStation=NDLS&nHr=2&jToStation=
```

| Parameter | Sample Value | Description |
| :--- | :--- | :--- |
| `service` | `TrainRunningMob` | Service identifier (fixed) |
| `subService` | `TrainsAtStationJson` | Subservice endpoint name |
| `jStation` | `NDLS` | Station code (e.g. New Delhi) |
| `nHr` | `2` | Time window in hours (e.g. '2' or '4') |
| `jToStation` | `` | Optional destination filter station code |

### Response JSON Sample (Decrypted)
```json
{
  "Station": "NDLS",
  "StationName": "NEW DELHI",
  "ToStation": "",
  "TrainsAtStation": [
    {
      "Destination": "JIND",
      "STA": "SRC",
      "DelayArr": "RT",
      "STD": "09:40 19-Oct",
      "depPWDCoachPosition": "",
      "Platform": "1",
      "TrainName": "NDLS-JIND MEMU",
      "Cancel": 0,
      "TrainTypeDescHindi": "मेमू",
      "DestinationHindiName": "जींद जं",
      "DstnChange": 0,
      "SourceHindiName": "नई दिल्ली",
      "Source": "NDLS",
      "StartDate": "19-Oct-2026",
      "DelayDep": "RT",
      "departureCoachPosition": "GEN-GEN-GEN-GEN-GEN-GEN-GEN-GEN",
      "TrainHindiName": "्",
      "DestinationName": "JIND JN",
      "DepCancelFlag": 0,
      "TrainType": "MEMU",
      "arrivalCoachClass": "GEN-GEN-GEN-GEN-GEN-GEN-GEN-GEN",
      "TrainNumber": "64915",
      "ETAFlag": true,
      "reversalFlag": false,
      "ArrFlag": "0",
      "SourceName": "NEW DELHI",
      "Diverted": 0,
      "departureCoachClass": "GEN-GEN-GEN-GEN-GEN-GEN-GEN-GEN",
      "SrcChange": 0,
      "ArrCancelFlag": 0,
      "ETA": "SRC",
      "arrPWDCoachPosition": "",
      "ETD": "09:40",
      "arrivalCoachPosition": "GEN-GEN-GEN-GEN-GEN-GEN-GEN-GEN",
      "DepFlag": "0",
      "TrainTypeDesc": "MEMU"
    },
    {
      "Destination": "GZB",
      "STA": "SRC",
      "DelayArr": "RT",
      "STD": "09:55 19-Oct",
      "depPWDCoachPosition": "",
      "Platform": "13",
      "TrainName": "NDLS-GZB EMU",
      "Cancel": 0,
      "TrainTypeDescHindi": "ई. एम. यू.",
      "DestinationHindiName": "गाजियाबाद",
      "DstnChange": 0,
      "SourceHindiName": "नई दिल्ली",
      "Source": "NDLS",
      "StartDate": "19-Oct-2026",
      "DelayDep": "RT",
      "departureCoachPosition": "GEN-GEN-GEN-GEN-GEN-GEN-GEN-GEN",
      "TrainHindiName": "इ",
      "DestinationName": "GHAZIABAD",
      "DepCancelFlag": 0,
      "TrainType": "EMU",
      "arrivalCoachClass": "GEN-GEN-GEN-GEN-GEN-GEN-GEN-GEN",
      "TrainNumber": "64426",
      "ETAFlag": true,
      "reversalFlag": false,
      "ArrFlag": "0",
      "SourceName": "NEW DELHI",
      "Diverted": 0,
      "departureCoachClass": "GEN-GEN-GEN-GEN-GEN-GEN-GEN-GEN",
      "SrcChange": 0,
      "ArrCancelFlag": 0,
      "ETA": "SRC",
      "arrPWDCoachPosition": "",
      "ETD": "09:55",
      "arrivalCoachPosition": "GEN-GEN-GEN-GEN-GEN-GEN-GEN-GEN",
      "DepFlag": "0",
      "TrainTypeDesc": "EMU"
    }
  ],
  "StationHindiName": "नई दिल्ली",
  "AlertMsgHindi": "",
  "AlertMsg": "",
  "ToStationName": "",
  "ToStationHindiName": "",
  "NextHr": "2",
  "TotalTrains": 45
}
```

### Field Dictionary for DAO / DTO Modeling

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `Station` | `String` | Text value / formatted string |
| `StationName` | `String` | Text value / formatted string |
| `ToStation` | `String` | Text value / formatted string |
| `TrainsAtStation` | `List<Object>` | Array of nested objects (see nested schema below) |
| `StationHindiName` | `String` | Text value / formatted string |
| `AlertMsgHindi` | `String` | Text value / formatted string |
| `AlertMsg` | `String` | Text value / formatted string |
| `ToStationName` | `String` | Text value / formatted string |
| `ToStationHindiName` | `String` | Text value / formatted string |
| `NextHr` | `String` | Text value / formatted string |
| `TotalTrains` | `Int` | Integer value |

#### Nested Items in `TrainsAtStation`

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `Destination` | `String` | Sample: `JIND` |
| `STA` | `String` | Sample: `SRC` |
| `DelayArr` | `String` | Sample: `RT` |
| `STD` | `String` | Sample: `09:40 19-Oct` |
| `depPWDCoachPosition` | `String` | Sample: `` |
| `Platform` | `String` | Sample: `1` |
| `TrainName` | `String` | Sample: `NDLS-JIND MEMU` |
| `Cancel` | `Int` | Sample: `0` |
| `TrainTypeDescHindi` | `String` | Sample: `मेमू` |
| `DestinationHindiName` | `String` | Sample: `जींद जं` |
| `DstnChange` | `Int` | Sample: `0` |
| `SourceHindiName` | `String` | Sample: `नई दिल्ली` |
| `Source` | `String` | Sample: `NDLS` |
| `StartDate` | `String` | Sample: `19-Oct-2026` |
| `DelayDep` | `String` | Sample: `RT` |
| `departureCoachPosition` | `String` | Sample: `GEN-GEN-GEN-GEN-GEN-GEN-GEN-GEN` |
| `TrainHindiName` | `String` | Sample: `्` |
| `DestinationName` | `String` | Sample: `JIND JN` |
| `DepCancelFlag` | `Int` | Sample: `0` |
| `TrainType` | `String` | Sample: `MEMU` |
| `arrivalCoachClass` | `String` | Sample: `GEN-GEN-GEN-GEN-GEN-GEN-GEN-GEN` |
| `TrainNumber` | `String` | Sample: `64915` |
| `ETAFlag` | `Boolean` | Sample: `True` |
| `reversalFlag` | `Boolean` | Sample: `False` |
| `ArrFlag` | `String` | Sample: `0` |
| `SourceName` | `String` | Sample: `NEW DELHI` |
| `Diverted` | `Int` | Sample: `0` |
| `departureCoachClass` | `String` | Sample: `GEN-GEN-GEN-GEN-GEN-GEN-GEN-GEN` |
| `SrcChange` | `Int` | Sample: `0` |
| `ArrCancelFlag` | `Int` | Sample: `0` |
| `ETA` | `String` | Sample: `SRC` |
| `arrPWDCoachPosition` | `String` | Sample: `` |
| `ETD` | `String` | Sample: `09:40` |
| `arrivalCoachPosition` | `String` | Sample: `GEN-GEN-GEN-GEN-GEN-GEN-GEN-GEN` |
| `DepFlag` | `String` | Sample: `0` |
| `TrainTypeDesc` | `String` | Sample: `MEMU` |

---

## 10. Trains Between Two Stations

**Endpoint (`subService`)**: `TrainBtwStnJson`  
**Description**: Returns list of all trains running between an origin station and a destination station with departure/arrival times, travel duration, and days of run.

### Request Specification

**Plain Query String:**
```text
service=TrainRunningMob&subService=TrainBtwStnJson&stnFrom=G&stnTo=NGP&trainType=ALL
```

| Parameter | Sample Value | Description |
| :--- | :--- | :--- |
| `service` | `TrainRunningMob` | Service identifier (fixed) |
| `subService` | `TrainBtwStnJson` | Subservice endpoint name |
| `stnFrom` | `G` | Source station code |
| `stnTo` | `NGP` | Destination station code |
| `trainType` | `ALL` | Train type filter ('ALL', 'EXP', etc.) |

### Response JSON Sample (Decrypted)
```json
{
  "Trains": [],
  "StationToHindi": "",
  "StationToCode": "BCT",
  "TrainTypeDescHindi": "सभी प्रकार के",
  "AlertMsg": "",
  "TotalTrains": 0,
  "StationTo": "BCT",
  "AlertMsgHindi": "",
  "StationFrom": "NEW DELHI",
  "StationFromHindi": "नई दिल्ली",
  "TrainTypeDesc": "All Types",
  "TrainType": "XXX",
  "StationFromCode": "NDLS",
  "RecordCount": ""
}
```

### Field Dictionary for DAO / DTO Modeling

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `Trains` | `List<Any>` | Array of elements |
| `StationToHindi` | `String` | Text value / formatted string |
| `StationToCode` | `String` | Text value / formatted string |
| `TrainTypeDescHindi` | `String` | Text value / formatted string |
| `AlertMsg` | `String` | Text value / formatted string |
| `TotalTrains` | `Int` | Integer value |
| `StationTo` | `String` | Text value / formatted string |
| `AlertMsgHindi` | `String` | Text value / formatted string |
| `StationFrom` | `String` | Text value / formatted string |
| `StationFromHindi` | `String` | Text value / formatted string |
| `TrainTypeDesc` | `String` | Text value / formatted string |
| `TrainType` | `String` | Text value / formatted string |
| `StationFromCode` | `String` | Text value / formatted string |
| `RecordCount` | `String` | Text value / formatted string |

---

## 11. Station Timetable

**Endpoint (`subService`)**: `GetStationTimeTable`  
**Description**: Returns the complete scheduled timetable of all trains passing through, arriving at, or originating from a specific station on a given date.

### Request Specification

**Plain Query String:**
```text
service=TrainRunningMob&subService=GetStationTimeTable&jStation=NDLS&startDate=19-Oct-2026
```

| Parameter | Sample Value | Description |
| :--- | :--- | :--- |
| `service` | `TrainRunningMob` | Service identifier (fixed) |
| `subService` | `GetStationTimeTable` | Subservice endpoint name |
| `jStation` | `NDLS` | Station code |
| `startDate` | `19-Oct-2026` | Schedule date (dd-MMM-yyyy) |

### Response JSON Sample (Decrypted)
```json
{
  "Station": "NDLS",
  "StationName": "NEW DELHI -NDLS",
  "StationHindiName": "नई दिल्ली -NDLS",
  "AlertMsgHindi": "",
  "AlertMsg": "",
  "vTrains": [
    {
      "Destination": "GZB",
      "STA": "SRC",
      "TrainNumber": "64422",
      "STD": "00:10",
      "Platform": "6",
      "TrainName": "NDLS-GZB EMU",
      "TrainTypeDescHindi": "ई. एम. यू.",
      "DestinationHindiName": "गाजियाबाद",
      "SourceName": "NEW DELHI",
      "ClassOfTravel": "GEN",
      "SourceHindiName": "नई दिल्ली",
      "Source": "NDLS",
      "ArrCancel": false,
      "DepCancel": false,
      "TrainHindiName": "्",
      "DaysOfDep": "",
      "DestinationName": "GHAZIABAD",
      "TrainTypeDesc": "EMU",
      "TrainType": "EMU"
    },
    {
      "Destination": "PWL",
      "STA": "SRC",
      "TrainNumber": "64076",
      "STD": "00:15",
      "Platform": "9",
      "TrainName": "NDLS-PWL EMU",
      "TrainTypeDescHindi": "ई. एम. यू.",
      "DestinationHindiName": "पलवल",
      "SourceName": "NEW DELHI",
      "ClassOfTravel": "GEN",
      "SourceHindiName": "नई दिल्ली",
      "Source": "NDLS",
      "ArrCancel": false,
      "DepCancel": false,
      "TrainHindiName": "्",
      "DaysOfDep": "",
      "DestinationName": "PALWAL",
      "TrainTypeDesc": "EMU",
      "TrainType": "EMU"
    }
  ]
}
```

### Field Dictionary for DAO / DTO Modeling

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `Station` | `String` | Text value / formatted string |
| `StationName` | `String` | Text value / formatted string |
| `StationHindiName` | `String` | Text value / formatted string |
| `AlertMsgHindi` | `String` | Text value / formatted string |
| `AlertMsg` | `String` | Text value / formatted string |
| `vTrains` | `List<Object>` | Array of nested objects (see nested schema below) |

#### Nested Items in `vTrains`

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `Destination` | `String` | Sample: `GZB` |
| `STA` | `String` | Sample: `SRC` |
| `TrainNumber` | `String` | Sample: `64422` |
| `STD` | `String` | Sample: `00:10` |
| `Platform` | `String` | Sample: `6` |
| `TrainName` | `String` | Sample: `NDLS-GZB EMU` |
| `TrainTypeDescHindi` | `String` | Sample: `ई. एम. यू.` |
| `DestinationHindiName` | `String` | Sample: `गाजियाबाद` |
| `SourceName` | `String` | Sample: `NEW DELHI` |
| `ClassOfTravel` | `String` | Sample: `GEN` |
| `SourceHindiName` | `String` | Sample: `नई दिल्ली` |
| `Source` | `String` | Sample: `NDLS` |
| `ArrCancel` | `Boolean` | Sample: `False` |
| `DepCancel` | `Boolean` | Sample: `False` |
| `TrainHindiName` | `String` | Sample: `्` |
| `DaysOfDep` | `String` | Sample: `` |
| `DestinationName` | `String` | Sample: `GHAZIABAD` |
| `TrainTypeDesc` | `String` | Sample: `EMU` |
| `TrainType` | `String` | Sample: `EMU` |

---

## 12. Average Delay History Statistics

**Endpoint (`subService`)**: `GetAvgDelayJson`  
**Description**: Returns historical average delay records across each station along the route for punctuality analytics.

### Request Specification

**Plain Query String:**
```text
service=TrainRunningMob&subService=GetAvgDelayJson&trainNo=11040
```

| Parameter | Sample Value | Description |
| :--- | :--- | :--- |
| `service` | `TrainRunningMob` | Service identifier (fixed) |
| `subService` | `GetAvgDelayJson` | Subservice endpoint name |
| `trainNo` | `11040` | Train number |

### Response JSON Sample (Decrypted)
```json
{
  "TrainNameHindi": "महारष्थ्र एक्सप्रेस्",
  "DstnNameHindi": "कोल्हापुर",
  "TypeName": "MAIL EXPRESS",
  "TrainName": "MAHARASHTRA EXP",
  "Src": "G",
  "AlertMsg": "",
  "SrcNameHindi": "गोंदिया जं",
  "Dstn": "KOP",
  "SrcName": "GONDIA JN",
  "DaysOfRun": "DAILY",
  "vAvgDelayList": [
    {
      "stnName": "GONDIA JN",
      "stnNameHindi": "गोंदिया जं",
      "stnArrDelay": "",
      "stn": "G",
      "stnDepDelay": "On Time",
      "sr": 1
    },
    {
      "stnName": "TIRORA",
      "stnNameHindi": "टिरोरा",
      "stnArrDelay": "00:12",
      "stn": "TRO",
      "stnDepDelay": "00:12",
      "sr": 2
    }
  ],
  "Type": "MEX",
  "AlertMsgHindi": "",
  "TrainNo": "11040",
  "DstnName": "KOLHAPUR",
  "TypeNameHindi": "MEX"
}
```

### Field Dictionary for DAO / DTO Modeling

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `TrainNameHindi` | `String` | Text value / formatted string |
| `DstnNameHindi` | `String` | Text value / formatted string |
| `TypeName` | `String` | Text value / formatted string |
| `TrainName` | `String` | Text value / formatted string |
| `Src` | `String` | Text value / formatted string |
| `AlertMsg` | `String` | Text value / formatted string |
| `SrcNameHindi` | `String` | Text value / formatted string |
| `Dstn` | `String` | Text value / formatted string |
| `SrcName` | `String` | Text value / formatted string |
| `DaysOfRun` | `String` | Text value / formatted string |
| `vAvgDelayList` | `List<Object>` | Array of nested objects (see nested schema below) |
| `Type` | `String` | Text value / formatted string |
| `AlertMsgHindi` | `String` | Text value / formatted string |
| `TrainNo` | `String` | Text value / formatted string |
| `DstnName` | `String` | Text value / formatted string |
| `TypeNameHindi` | `String` | Text value / formatted string |

#### Nested Items in `vAvgDelayList`

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `stnName` | `String` | Sample: `GONDIA JN` |
| `stnNameHindi` | `String` | Sample: `गोंदिया जं` |
| `stnArrDelay` | `String` | Sample: `` |
| `stn` | `String` | Sample: `G` |
| `stnDepDelay` | `String` | Sample: `On Time` |
| `sr` | `Int` | Sample: `1` |

---

## 13. Train Exception / Alert Info

**Endpoint (`subService`)**: `TrainExcpInfo`  
**Description**: Returns exceptions, cancellations, diversions, or rescheduling notices for a given train.

### Request Specification

**Plain Query String:**
```text
service=TrainRunningMob&subService=TrainExcpInfo&trainNo=11040
```

| Parameter | Sample Value | Description |
| :--- | :--- | :--- |
| `service` | `TrainRunningMob` | Service identifier (fixed) |
| `subService` | `TrainExcpInfo` | Subservice endpoint name |
| `trainNo` | `11040` | Train number |

### Response JSON Sample (Decrypted)
```json
{
  "TrainNameHindi": "महारष्थ्र एक्सप्रेस्",
  "Destination": "KOP",
  "TrainNumber": "11040",
  "AlertMsgHindi": "No Exceptional Details found for train 11040 !!!",
  "TrainName": "MAHARASHTRA EXP",
  "SourceNameHindi": "गोंदिया जं",
  "AlertMsg": "No Exceptional Details found for train 11040 !!!",
  "DestinationName": "KOLHAPUR",
  "SourceName": "GONDIA JN",
  "DaysOfRun": "DAILY",
  "DestinationNameHindi": "कोल्हापुर",
  "Source": "G"
}
```

### Field Dictionary for DAO / DTO Modeling

| Field Name | Data Type | Description / Notes |
| :--- | :--- | :--- |
| `TrainNameHindi` | `String` | Text value / formatted string |
| `Destination` | `String` | Text value / formatted string |
| `TrainNumber` | `String` | Text value / formatted string |
| `AlertMsgHindi` | `String` | Text value / formatted string |
| `TrainName` | `String` | Text value / formatted string |
| `SourceNameHindi` | `String` | Text value / formatted string |
| `AlertMsg` | `String` | Text value / formatted string |
| `DestinationName` | `String` | Text value / formatted string |
| `SourceName` | `String` | Text value / formatted string |
| `DaysOfRun` | `String` | Text value / formatted string |
| `DestinationNameHindi` | `String` | Text value / formatted string |
| `Source` | `String` | Text value / formatted string |

---
