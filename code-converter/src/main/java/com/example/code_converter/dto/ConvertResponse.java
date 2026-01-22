package com.example.code_converter.dto;


public class ConvertResponse {
    private String convertedCode;

    public ConvertResponse(String convertedCode){
        this.convertedCode = convertedCode;
    }
    public  String getConvertedCode(){
        return  convertedCode;
    }
}
