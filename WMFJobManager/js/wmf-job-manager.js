//-------------------------------------------------------------
// \project MeteoGIS JobManager
// \file mg-job-manager.js
// \brief 任务管理器
// \author 王庆飞
// \date 2016-6-14
// \attention
// Copyright(c) MeteoGIS Group
// All Rights Reserved
// \version 1.0
//-------------------------------------------------------------

if (typeof ExecuteStatus == "undefined") {
    var ExecuteStatus = {};
    ExecuteStatus.Invalid = 0;
    ExecuteStatus.Executing = 1;
    ExecuteStatus.Succeeded = 2;
    ExecuteStatus.Failed = 3;
    ExecuteStatus.Canceled = 4;
    ExecuteStatus.ExecutingAgain = 5;
    ExecuteStatus.SucceededAgain = 6;
    ExecuteStatus.FailedAgain = 7;
    ExecuteStatus.CanceledAgain = 8;
}

function Format(format, v) {
    var str = v.toString();
    return (format + str).substr(str.length);
}

function ExecuteStatusToString(es) {
    switch (es) {
        case ExecuteStatus.Invalid:
            return "无效状态";
        case ExecuteStatus.Executing:
            return "正在执行";
        case ExecuteStatus.Succeeded:
            return "成功";
        case ExecuteStatus.Failed:
            return "失败";
        case ExecuteStatus.Canceled:
            return "执行取消";
        case ExecuteStatus.ExecutingAgain:
            return "正在再次执行";
        case ExecuteStatus.SucceededAgain:
            return "再次执行成功";
        case ExecuteStatus.FailedAgain:
            return "再次执行失败";
        case ExecuteStatus.CanceledAgain:
            return "再次执行取消";
        default:
            return "";
    }
}

function ExecuteStatusToBackgroundColor(es) {
    switch (es) {
        case ExecuteStatus.Invalid:
            return "#808080";
        case ExecuteStatus.Executing:
            return "#006837";
        case ExecuteStatus.Succeeded:
            return "#0000ff";
        case ExecuteStatus.Failed:
            return "#ed1c24";
        case ExecuteStatus.Canceled:
            return "#93278f";
        case ExecuteStatus.ExecutingAgain:
            return "#006837";
        case ExecuteStatus.SucceededAgain:
            return "#0000ff";
        case ExecuteStatus.FailedAgain:
            return "#ed1c24";
        case ExecuteStatus.CanceledAgain:
            return "#93278f";
        default:
            return "#808080";
    }
}

if (typeof Result == "undefined") {
    var Result = {};
    Result.OK = 0;
    Result.JobExisted = 1;
    Result.JobNotExisted = 2;
    Result.InvalidModel = 3;
    Result.InvalidCronExpression = 4;
}