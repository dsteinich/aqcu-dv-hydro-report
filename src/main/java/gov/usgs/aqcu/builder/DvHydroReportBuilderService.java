package gov.usgs.aqcu.builder;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import java.math.BigDecimal;
import java.time.Instant;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.Approval;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.ControlConditionActivity;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.ControlConditionType;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.DischargeActivity;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.DischargeSummary;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.DoubleWithDisplay;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.FieldVisitDataServiceResponse;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.FieldVisitDescription;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.FieldVisitDescriptionListServiceResponse;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.Qualifier;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.QualifierMetadata;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.QuantityWithDisplay;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.LocationDescription;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.LocationDescriptionListServiceResponse;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.TimeSeriesDataServiceResponse;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.TimeSeriesDescription;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.TimeSeriesDescriptionListByUniqueIdServiceResponse;
import com.aquaticinformatics.aquarius.sdk.timeseries.servicemodels.Publish.TimeSeriesPoint;

import gov.usgs.aqcu.model.*;
import gov.usgs.aqcu.parameter.DvHydroRequestParameters;
import gov.usgs.aqcu.retrieval.*;

@Component
public class DvHydroReportBuilderService {	
	private static final Logger LOG = LoggerFactory.getLogger(DvHydroReportBuilderService.class);
	private final String BASE_URL = "http://temp/report/";
	private TimeSeriesDataCorrectedService timeSeriesDataCorrectedService;
	private TimeSeriesDescriptionService timeSeriesDescriptionListService;
	private LocationDescriptionService locationDescriptionListService;
	private QualifierLookupService qualifierLookupService;
	private FieldVisitDescriptionService fieldVisitDescriptionListService; 
	private FieldVisitDataService fieldVisitDataService;
	private static final String ESTIMATED_QUALIFIER_VALUE = "ESTIMATED";
	private static final String GAP_MARKER_POINT_VALUE = "EMPTY";

	@Autowired
	public DvHydroReportBuilderService(
		TimeSeriesDataCorrectedService timeSeriesDataCorrectedService,
		TimeSeriesDescriptionService timeSeriesDescriptionListService,
		LocationDescriptionService locationDescriptionListService,
		QualifierLookupService qualifierLookupService,
		FieldVisitDescriptionService fieldVisitDescriptionListService,
		FieldVisitDataService fieldVisitDataService) {
		this.timeSeriesDataCorrectedService = timeSeriesDataCorrectedService;
		this.timeSeriesDescriptionListService = timeSeriesDescriptionListService;
		this.locationDescriptionListService = locationDescriptionListService;
		this.qualifierLookupService = qualifierLookupService;
		this.fieldVisitDescriptionListService = fieldVisitDescriptionListService;
		this.fieldVisitDataService = fieldVisitDataService;
	}

	public DvHydroReport buildReport(DvHydroRequestParameters requestParameters, String requestingUser) {
		DvHydroReport dvHydroReport = new DvHydroReport();

		Map<String, TimeSeriesDescription> timeSeriesDescriptions = timeSeriesDescriptionListService.getTimeSeriesDescriptions(requestParameters);

		dvHydroReport.setReportMetadata(createDvHydroMetadata(requestParameters, timeSeriesDescriptions, requestingUser));

		if (timeSeriesDescriptions.get(requestParameters.getPrimaryTimeseriesIdentifier()).getParameter().toString().contains("Discharge")) {
			dvHydroReport.setFieldVisitMeasurements(buildFieldVisitMeasurements(requestParameters, dvHydroReport.getReportMetadata().getStationId()));
		}

		return dvHydroReport;
	}

	protected DvHydroMetadata createDvHydroMetadata(DvHydroRequestParameters requestParameters, Map<String, TimeSeriesDescription> timeSeriesDescriptions, String requestingUser) {

		DvHydroMetadata metadata = new DvHydroMetadata();

		metadata.setRequestingUser(requestingUser);
		metadata.setTimezone("Etc/GMT" + (int)(-1 * timeSeriesDescriptions.get(requestParameters.getPrimaryTimeseriesIdentifier()).getUtcOffset()));
		metadata.setStartDate(requestParameters.getStartInstant());
		metadata.setEndDate(requestParameters.getEndInstant());
		metadata.setTitle("DV Hydrograph");
		metadata.setPrimaryParameter(timeSeriesDescriptions.get(requestParameters.getPrimaryTimeseriesIdentifier()).getIdentifier());

		if (timeSeriesDescriptions.containsKey(requestParameters.getFirstStatDerivedIdentifier())) {
			metadata.setFirstStatDerivedParameter(timeSeriesDescriptions.get(requestParameters.getFirstStatDerivedIdentifier()).getIdentifier());
		}
		if (timeSeriesDescriptions.containsKey(requestParameters.getSecondStatDerivedIdentifier())) {
			metadata.setSecondStatDerivedParameter(timeSeriesDescriptions.get(requestParameters.getSecondStatDerivedIdentifier()).getIdentifier());
		}
		if (timeSeriesDescriptions.containsKey(requestParameters.getThirdStatDerivedIdentifier())) {
			metadata.setThirdStatDerivedParameter(timeSeriesDescriptions.get(requestParameters.getThirdStatDerivedIdentifier()).getIdentifier());
		}
		if (timeSeriesDescriptions.containsKey(requestParameters.getFourthStatDerivedIdentifier())) {
			metadata.setFourthStatDerivedParameter(timeSeriesDescriptions.get(requestParameters.getFourthStatDerivedIdentifier()).getIdentifier());
		}
		if (timeSeriesDescriptions.containsKey(requestParameters.getFirstReferenceIdentifier())) {
			metadata.setFirstReferenceTimeSeriesParameter(timeSeriesDescriptions.get(requestParameters.getFirstReferenceIdentifier()).getIdentifier());
		}
		if (timeSeriesDescriptions.containsKey(requestParameters.getSecondReferenceIdentifier())) {
			metadata.setSecondReferenceTimeSeriesParameter(timeSeriesDescriptions.get(requestParameters.getSecondReferenceIdentifier()).getIdentifier());
		}
		if (timeSeriesDescriptions.containsKey(requestParameters.getThirdReferenceIdentifier())) {
			metadata.setThirdReferenceTimeSeriesParameter(timeSeriesDescriptions.get(requestParameters.getThirdReferenceIdentifier()).getIdentifier());
		}
		if (timeSeriesDescriptions.containsKey(requestParameters.getComparisonTimeseriesIdentifier())) {
			metadata.setComparisonSeriesParameter(timeSeriesDescriptions.get(requestParameters.getComparisonTimeseriesIdentifier()).getIdentifier());
		}

		metadata.setQualifierMetadata(qualifierLookupService.getByQualifierList(timeSeriesDataCorrectedService.getQualifiers(requestParameters)));

		LocationDescription locationDescription = locationDescriptionListService.getByLocationIdentifier(timeSeriesDescriptions.get(requestParameters.getPrimaryTimeseriesIdentifier()).getLocationIdentifier());
		metadata.setStationName(locationDescription.getName());
		metadata.setStationId(locationDescription.getIdentifier());

		return metadata;
	}

	protected List<AqcuFieldVisitMeasurement> buildFieldVisitMeasurements(DvHydroRequestParameters requestParameters, String locationIdentifier) {
		List<AqcuFieldVisitMeasurement> aqcuFieldVisitMeasurements = null;

		List<FieldVisitDescription> fieldVisitDescriptions = fieldVisitDescriptionListService.getDescriptions(requestParameters);

		if (fieldVisitDescriptions != null){
			List<AqcuFieldVisit> fieldVisits = createFieldVisits(fieldVisitDescriptions);

			aqcuFieldVisitMeasurements = new ArrayList<>();

			for (AqcuFieldVisit fvd : fieldVisits){
				aqcuFieldVisitMeasurements.addAll(createFieldVisitMeasurement(
						fvd, 
						null,
						false));
			}
		}

		return aqcuFieldVisitMeasurements;
	}

	private List<AqcuFieldVisit> createFieldVisits(List<FieldVisitDescription> fieldVisitDescriptions) {
		List<AqcuFieldVisit> fieldVisits = new ArrayList<>();

		for(FieldVisitDescription info : fieldVisitDescriptions) {
			fieldVisits.add(new AqcuFieldVisit(
					info.getLocationIdentifier(),
					info.getStartTime(), 
					info.getEndTime(), 
					info.getIdentifier(), 
					info.getIsValid(),
					info.getLastModified(), 
					info.getParty(), 
					info.getRemarks(), 
					info.getWeather()));
		}
		return fieldVisits;
	}








	private void temp() {
			TimeSeriesDataServiceResponse firstStatDerivedDataResponse = null;
			TimeSeriesDataServiceResponse secondStatDerivedDataResponse = null;
			TimeSeriesDataServiceResponse thirdStatDerivedDataResponse = null;
			TimeSeriesDataServiceResponse fourthStatDerivedDataResponse = null;
			TimeSeriesDataServiceResponse firstReferenceDataResponse = null;
			TimeSeriesDataServiceResponse secondReferenceDataResponse = null;
			TimeSeriesDataServiceResponse thirdReferenceDataResponse = null;
			TimeSeriesDataServiceResponse comparisonSeriesDataResponse = null;
			FieldVisitDescriptionListServiceResponse fieldVisitDescriptions = null;

//			boolean isDischarge = false;
//			try {
//				isDischarge = (Boolean) primaryDescription.getParameter().toString().contains("Discharge");
//			} catch (Exception e) {
//				LOG.trace("No discharge involved");
//			}
//		//Fetch Location Descriptions
//		LocationDescriptionListServiceResponse locationResponse = locationDescriptionListService.get(primaryDescription.getLocationIdentifier());
//		LocationDescription locationDescription = locationResponse.getLocationDescriptions().get(0);	
//
//		//Fetch Primary Series Data
//		TimeSeriesDataServiceResponse primarySeriesDataResponse = timeSeriesDataCorrectedService.get(primaryTimeseriesIdentifier, startDate, endDate);
//
//		//Fetch First Stat-Derived Series Data, if exists
//		if (firstStatDerivedIdentifier != null) {
//			firstStatDerivedDataResponse = timeSeriesDataCorrectedService.get(firstStatDerivedIdentifier, startDate, endDate);
//		}
//
//		//Fetch Second Stat-Derived Series Data, if exists
//		if (secondStatDerivedIdentifier != null) {
//			secondStatDerivedDataResponse = timeSeriesDataCorrectedService.get(secondStatDerivedIdentifier, startDate, endDate);
//		}
//
//		//Fetch Third Stat-Derived Series Data, if exists
//		if (thirdStatDerivedIdentifier != null) {
//			thirdStatDerivedDataResponse = timeSeriesDataCorrectedService.get(thirdStatDerivedIdentifier, startDate, endDate);
//		}
//
//		//Fetch Fourth Stat-Derived Series Data, if exists
//		if (fourthStatDerivedIdentifier != null) {
//			fourthStatDerivedDataResponse = timeSeriesDataCorrectedService.get(fourthStatDerivedIdentifier, startDate, endDate);
//		}
//
//		//Fetch First Reference Series Data, if exists
//		if (firstReferenceIdentifier != null) {
//			firstReferenceDataResponse = timeSeriesDataCorrectedService.get(firstReferenceIdentifier, startDate, endDate);
//		}
//
//		//Fetch Second Reference Series Data, if exists
//		if (secondReferenceIdentifier != null) {
//			secondReferenceDataResponse = timeSeriesDataCorrectedService.get(secondReferenceIdentifier, startDate, endDate);
//		}
//
//		//Fetch Third Reference Series Data, if exists
//		if (thirdReferenceIdentifier != null) {
//			thirdReferenceDataResponse = timeSeriesDataCorrectedService.get(thirdReferenceIdentifier, startDate, endDate);
//		}
//
//		//Fetch Comparison Series Data, if exists
//		if (comparisonTimeseriesIdentifier != null) {
//			comparisonSeriesDataResponse = timeSeriesDataCorrectedService.get(comparisonTimeseriesIdentifier, startDate, endDate);
//		}
//
//		//Additional Metadata Lookups
//		List<QualifierMetadata> qualifierMetadataList = qualifierLookupService.getByQualifierList(primarySeriesDataResponse.getQualifiers());
//
//		List<Qualifier> qualifierList = primarySeriesDataResponse.getQualifiers();
//
//		List<Approval> approvalList = primarySeriesDataResponse.getApprovals();
//
		DvHydroReport report = createReport(
			primarySeriesDataResponse, 
			primaryDescription, 
			firstStatDerivedDataResponse,
			secondStatDerivedDataResponse,
			thirdStatDerivedDataResponse,
			fourthStatDerivedDataResponse,
			firstReferenceDataResponse,
			secondReferenceDataResponse,
			thirdReferenceDataResponse,
			comparisonSeriesDataResponse,
			firstStatDerivedDescription,
			secondStatDerivedDescription,
			thirdStatDerivedDescription,
			fourthStatDerivedDescription,
			firstReferenceDescription,
			secondReferenceDescription,
			thirdReferenceDescription,
			comparisonTimeseriesDescription,
			locationDescription,
			approvalList,
			qualifierList,
			qualifierMetadataList,
			fieldVisitDescriptions,
			startDate, 
			endDate, 
			requestingUser);
//
//		return report;
	}

	private DvHydroReport createReport (
		TimeSeriesDataServiceResponse primaryDataResponse,
		TimeSeriesDescription primaryDescription,
		TimeSeriesDataServiceResponse firstStatDerivedDataResponse,
		TimeSeriesDataServiceResponse secondStatDerivedDataResponse,
		TimeSeriesDataServiceResponse thirdStatDerivedDataResponse,
		TimeSeriesDataServiceResponse fourthStatDerivedDataResponse,
		TimeSeriesDataServiceResponse firstReferenceDataResponse,
		TimeSeriesDataServiceResponse secondReferenceDataResponse,
		TimeSeriesDataServiceResponse thirdReferenceDataResponse,
		TimeSeriesDataServiceResponse comparisonSeriesDataResponse,
		TimeSeriesDescription firstStatDerivedDescription,
		TimeSeriesDescription secondStatDerivedDescription,
		TimeSeriesDescription thirdStatDerivedDescription,
		TimeSeriesDescription fourthStatDerivedDescription,
		TimeSeriesDescription firstReferenceDescription,
		TimeSeriesDescription secondReferenceDescription,
		TimeSeriesDescription thirdReferenceDescription,
		TimeSeriesDescription comparisonTimeseriesDescription,
		LocationDescription locationDescription,
		List<Approval> approvalList,
		List<Qualifier> qualifierList,
		List<QualifierMetadata> qualifierMetadataList,
		FieldVisitDescriptionListServiceResponse fieldVisitDescriptions,
		Instant startDate,
		Instant endDate,
		String requestingUser) throws Exception {
			DvHydroReport report = new DvHydroReport();

			//Add Primary TS Metadata
			report.setPrimaryTsMetadata(primaryDescription);

			//Add First Stat-Derived Data, if exists
			if (firstStatDerivedDataResponse != null) {
				report.setFirstStatDerivedData(createDvHydroCorrectedData(firstStatDerivedDataResponse, startDate, endDate));
			}

			//Add Second Stat-Derived Data, if exists
			if (secondStatDerivedDataResponse != null) {
				report.setFirstStatDerivedData(createDvHydroCorrectedData(secondStatDerivedDataResponse, startDate, endDate));
			}

			//Add Third Stat-Derived Data, if exists
			if (thirdStatDerivedDataResponse != null) {
				report.setFirstStatDerivedData(createDvHydroCorrectedData(thirdStatDerivedDataResponse, startDate, endDate));
			}

			//Add Fourth Stat-Derived Data, if exists
			if (fourthStatDerivedDataResponse != null) {
				report.setFirstStatDerivedData(createDvHydroCorrectedData(firstStatDerivedDataResponse, startDate, endDate));
			}

			report.setQualifier(qualifierList);

			report.setApproval(approvalList);
			return report;
	}

	private DvHydroCorrectedData createDvHydroCorrectedData(TimeSeriesDataServiceResponse response, Instant startDate, Instant endDate) {
		DvHydroCorrectedData data = new DvHydroCorrectedData();

		/*  Getting date errors, don't feel like dealing with it yet
		List<DateRange> estimatedPeriods = new ArrayList<>();
		for(Qualifier q : response.getQualifiers()) {
			if(q.getIdentifier().equals(ESTIMATED_QUALIFIER_VALUE)) {
				estimatedPeriods.add(new DateRange(q.getStartTime(), q.getEndTime()));
			}
		}
		*/
		//Copy Desired Fields
		data.setUnit(response.getUnit());
		data.setType(response.getParameter());
		data.setPoints(createDvHydroPoints(response.getPoints()));
		
		/*
		if(estimatedPeriods.size() > 0) {
			data.setEstimatedPeriods(estimatedPeriods);
		}
		*/

		return data;
	}

	private List<AqcuPoint> createDvHydroPoints(List<TimeSeriesPoint> timeSeriesPoints){
		List<AqcuPoint> dvPoints =  new ArrayList<>();
		for(TimeSeriesPoint pt : timeSeriesPoints) {
			AqcuPoint dvPoint = new AqcuPoint();
			dvPoint.setTime(pt.getTimestamp().getDateTimeOffset());
			dvPoint.setValue(getRoundedValue(pt.getValue()));
			dvPoints.add(dvPoint);
		}
		return dvPoints;
	}

	/**
	 * Converts Aquarius Field Visits to AQCU FieldVisit objects
	 * 
	 * @param visitsData The Aquarius Field Visit service response
	 * @return The list of converted FieldVisit obejcts
	 */
	private List<AqcuFieldVisitMeasurement> createFieldVisitMeasurement(
			AqcuFieldVisit visit, 
			String ratingModelIdentifier,
			Boolean excludeIceCover) {
		List<AqcuFieldVisitMeasurement> ret = new ArrayList<AqcuFieldVisitMeasurement>();

		FieldVisitDataServiceResponse fieldVisitDataServiceResponse = fieldVisitDataService.get(visit.getIdentifier());

		if(excludeIceCover != null && excludeIceCover && isIceCover(fieldVisitDataServiceResponse)) {
			return ret;
		}

		ControlConditionType controlCondition =  fieldVisitDataServiceResponse.getControlConditionActivity() != null ? fieldVisitDataServiceResponse.getControlConditionActivity().getControlCondition() : null;

		String controlConditionString = controlCondition != null ? controlCondition.toString() : null;

		if (fieldVisitDataServiceResponse.getDischargeActivities() != null) {
			for (DischargeActivity disActivity : fieldVisitDataServiceResponse.getDischargeActivities()) {
				DischargeSummary dischargeSummary = disActivity.getDischargeSummary();
				if (dischargeSummary != null) {
					QuantityWithDisplay discharge = dischargeSummary.getDischarge(); 
					if (dischargeSummary.getDischarge() != null) {
						AqcuFieldVisitMeasurement.MeasurementGrade grade = AqcuFieldVisitMeasurement.MeasurementGrade.valueFrom(dischargeSummary.getMeasurementGrade().name());
						String measurementNumber = dischargeSummary.getMeasurementId();
						AqcuFieldVisitMeasurement errorBar = grade.calculateError(measurementNumber, controlConditionString, 
								getRoundedValue(discharge), dischargeSummary.getDischarge().getUnit(),
								dischargeSummary.getMeanGageHeight().getUnit(), dischargeSummary.getMeasurementStartTime(), 
								visit.getIdentifier());
						errorBar.setMeanGageHeight(getRoundedValue(dischargeSummary.getMeanGageHeight()));
						errorBar.setRatingModelIdentifier(ratingModelIdentifier);
						ret.add(errorBar);
					}
				}
			}
		}

		return ret;
	}

	/**
	 * Returns whether or not the supplied field visit had ice cover when it was performed
	 * @param fieldVisitDataResponse The Aquarius field visit service response to check
	 * @return TRUE - Ice Cover was present | FALSE - Ice Cover was not present
	 */
	public boolean isIceCover(FieldVisitDataServiceResponse fieldVisitDataResponse) {
		boolean isIceCover = false;
		ControlConditionActivity c = fieldVisitDataResponse.getControlConditionActivity();

		if(c != null) {
			for(ControlConditionType iceCoverType : ICE_COVER_CONTROL_CONDITIONS) {
				if(c.getControlCondition().equals(iceCoverType)) {
					isIceCover = true;
					break;
				}
			}
		}

		return isIceCover;
	}

	private BigDecimal getRoundedValue(DoubleWithDisplay referenceVal){
		BigDecimal ret;

		if (referenceVal != null) {
			String tmp = referenceVal.getDisplay();
			if (tmp == null || tmp.equals(GAP_MARKER_POINT_VALUE)) {
				ret = BigDecimal.valueOf(referenceVal.getNumeric()); //Should be null but just in case.
			} else {
				ret = new BigDecimal(tmp);
			}
		} else {
			ret = null;
		}
		return ret;
	}

	private static final List<ControlConditionType> ICE_COVER_CONTROL_CONDITIONS = Arrays.asList(new ControlConditionType[] {
			ControlConditionType.IceCover,
			ControlConditionType.IceShore,
			ControlConditionType.IceAnchor
	});

	private String createReportURL(String reportType, Map<String, String> parameters) {
		String reportUrl = BASE_URL + reportType + "?";
		
		for(Map.Entry<String, String> entry : parameters.entrySet()) {
			if(entry.getKey() != null && entry.getKey().length() > 0) {
				reportUrl += entry.getKey() + "=" + entry.getValue() + "&";
			}
		}
		
		return reportUrl.substring(0, reportUrl.length()-1);
	}

}
