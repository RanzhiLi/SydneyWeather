package org.asdtm.goodweather.model;

import android.text.TextUtils;

public class CitySearch
{
    private String mCityName = "Sydney";
    private String mCountry="AU";
    private String mLatitude="33.51";
    private String mLongitude="151.12";
    private String mCountryCode="AU";

    public CitySearch(){}
    public CitySearch(String cityName, String countryCode, String latitude, String longitude)
    {
        mCityName = cityName;
        mCountryCode = countryCode;
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public String getCityName()
    {
        return TextUtils.isEmpty(mCityName)?"Sydney":mCityName;
    }

    public void setCityName(String cityName)
    {
        mCityName = cityName;
    }

    public String getCountry()
    {
        return mCountry;
    }

    public void setCountry(String country)
    {
        mCountry = country;
    }

    public String getLatitude()
    {
        return mLatitude;
    }

    public void setLatitude(String latitude)
    {
        mLatitude = latitude;
    }

    public String getLongitude()
    {
        return mLongitude;
    }

    public void setLongitude(String longitude)
    {
        mLongitude = longitude;
    }

    public String getCountryCode()
    {
        return mCountryCode;
    }

    public void setCountryCode(String countryCode)
    {
        mCountryCode = countryCode;
    }

    @Override
    public String toString()
    {
        return mCityName + ", " + mCountryCode;
    }
}
