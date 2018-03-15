package gov.usgs.aqcu.model;

import static gov.usgs.aqcu.util.ScientificArithmetic.add;
import static gov.usgs.aqcu.util.ScientificArithmetic.multiply;
import static gov.usgs.aqcu.util.ScientificArithmetic.subtract;

import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;

/**
 * The AQCU representation of a Field Visit Measurement
 * 
 * @author thongsav, kmschoep
 */
public class AqcuFieldVisitMeasurement {

	//shift (input values from discharge
	private BigDecimal shiftInFeet;
	private BigDecimal errorMinShiftInFeet;
	private BigDecimal errorMaxShiftInFeet;

	private String identifier;
	private String controlCondition;
	private Instant measurementStartDate;
	private String ratingModelIdentifier;

	//discharge
	private BigDecimal discharge;
	private String dischargeUnits;
	private BigDecimal errorMinDischarge;
	private BigDecimal errorMaxDischarge;

	private String measurementNumber;
	private AqcuFieldVisitMeasurement.MeasurementGrade qualityRating;
	private boolean historic;
	private BigDecimal meanGageHeight;
	private String meanGageHeightUnits;
	private int shiftNumber;

	public AqcuFieldVisitMeasurement(){
	}
	
	/**
	 * Constructor that creates an AQCU FieldVisitMeasurement with all of the necessary and
	 * relevant  parameters.
	 * 
	 * @param inMeasurementNumber The actual measurement number
	 * @param controlCondition The control conditions related to this measurement
	 * @param inDischargeValue The discharge value associated with this measurement
	 * @param inDischargeUnits The units associated with the discharge value
	 * @param meanGageHeightUnits The units associated with the meanGageHeight value
	 * @param inErrorMaxDischarge The max error in the discharge value
	 * @param inErrorMinDischarge The min error in the discharge value
	 * @param grade The grade of this measurement
	 * @param inMeasurementStartDate The date & time when this measurement was beginning to be taken
	 * @param inIdentifier The unique identifier associated with this field visit measurement
	 */
	public AqcuFieldVisitMeasurement(String inMeasurementNumber, String controlCondition, BigDecimal inDischargeValue, String inDischargeUnits, String meanGageHeightUnits, 
			BigDecimal inErrorMaxDischarge, BigDecimal inErrorMinDischarge, AqcuFieldVisitMeasurement.MeasurementGrade grade, 
			Instant inMeasurementStartDate, String inIdentifier) {
		measurementNumber = inMeasurementNumber;
		this.controlCondition = controlCondition;
		this.discharge = inDischargeValue;
		this.dischargeUnits = inDischargeUnits;
		this.meanGageHeightUnits = meanGageHeightUnits;
		this.errorMinDischarge = inErrorMinDischarge;
		this.errorMaxDischarge = inErrorMaxDischarge;
		this.qualityRating = grade;
		this.measurementStartDate = inMeasurementStartDate;
		this.identifier = inIdentifier;
	}

	/**
	 * A utility function that calculates the error shifts in this measurement
	 * based on the provided error values and the existing measurement values.
	 * 
	 * @param inputValues The error values to apply to the measurement values in order to calculate the shifts
	 */
	public void calculateShifts(List<BigDecimal> inputValues) {
		if (inputValues.size() > 0 && inputValues.get(0) != null) {
			errorMaxShiftInFeet = subtract(inputValues.get(0), meanGageHeight);
		}
		if (inputValues.size() > 1 && inputValues.get(1) != null) {
			shiftInFeet = subtract(inputValues.get(1), meanGageHeight);
		}
		if (inputValues.size() > 2 && inputValues.get(2) != null) {
			errorMinShiftInFeet = subtract(inputValues.get(2), meanGageHeight);
		}
	}
	
	/**
	 *
	 * @return The unique identifier
	 */
	public String getIdentifier() {
		return identifier;
	}
	
	/**
	 *
	 * @param historic Sets whether or not this is a historic measurement
	 */
	public void setHistoric(boolean historic) {
		this.historic = historic;
	}

	/**
	 *
	 * @return The shift of the measurement in feet
	 */
	public BigDecimal getShiftInFeet() {
		return shiftInFeet;
	}

	/**
	 *
	 * @return The min error of the shift in feet
	 */
	public BigDecimal getErrorMinShiftInFeet() {
		return errorMinShiftInFeet;
	}

	/**
	 *
	 * @return The max error of the shift in feet
	 */
	public BigDecimal getErrorMaxShiftInFeet() {
		return errorMaxShiftInFeet;
	}

	/**
	 *
	 * @return The discharge measurement
	 */
	public BigDecimal getDischarge() {
		return discharge;
	}
	
	/**
	 *
	 * @return The units associated with the discharge
	 */
	public String getDischargeUnits() {
		return dischargeUnits;
	}

	/**
	 *
	 * @return The min error of the discharge in feet
	 */
	public BigDecimal getErrorMinDischargeInFeet() {
		return errorMinDischarge;
	}

	/**
	 *
	 * @return The max error of the discharge in feet
	 */
	public BigDecimal getErrorMaxDischargeInFeet() {
		return errorMaxDischarge;
	}

	/**
	 *
	 * @return The actual measurement number
	 */
	public String getMeasurementNumber() {
		return measurementNumber;
	}

	/**
	 *
	 * @return The measurement grade's associated quality rating
	 */
	public AqcuFieldVisitMeasurement.MeasurementGrade getQualityRating() {
		return qualityRating;
	}

	/**
	 *
	 * @return Whether or not the measurement is historic
	 */
	public boolean isHistoric() {
		return historic;
	}

	/**
	 *
	 * @param value The gage height to set
	 */
	public void setMeanGageHeight(BigDecimal value) {
		meanGageHeight = value;
	}

	/**
	 *
	 * @return The mean gage height
	 */
	public BigDecimal getMeanGageHeight() {
		return meanGageHeight;
	}

	/**
	 *
	 * @return The units associated with the mean gage height
	 */
	public String getMeanGageHeightUnits() {
		return meanGageHeightUnits;
	}

	/**
	 *
	 * @param meanGageHeightUnits The units to associate with the mean gage height
	 */
	public void setMeanGageHeightUnits(String meanGageHeightUnits) {
		this.meanGageHeightUnits = meanGageHeightUnits;
	}

	/**
	 *
	 * @return The date & time when this measurement was beginning to be taken
	 */
	public Instant getMeasurementStartDate() {
		return measurementStartDate;
	}

	/**
	 *
	 * @return A single row vector containing the errorMaxDischarge, discharge, and errorMinDischarge
	 */
	public BigDecimal[] getOutputValues() {
		return new BigDecimal[]{errorMaxDischarge, discharge, errorMinDischarge};
	}

	/**
	 *
	 * @return The unique identifier of the associated rating model
	 */
	public String getRatingModelIdentifier() {
		return ratingModelIdentifier;
	}

	/**
	 *
	 * @param inRatingModelIdentifier The unique identifier to set to specify the associated rating model
	 */
	public void setRatingModelIdentifier(String inRatingModelIdentifier) {
		ratingModelIdentifier = inRatingModelIdentifier;
	}

	/**
	 *
	 * @param inShiftNumber The shift number to set
	 */
	public void setShiftNumber(int inShiftNumber) {
		shiftNumber = inShiftNumber;
	}

	/**
	 *
	 * @return The associated shift number
	 */
	public int getShiftNumber() {
		return shiftNumber;
	}

	/**
	 *
	 * @return The control condition
	 */
	public String getControlCondition() {
		return controlCondition;
	}

	/**
	 * Represents anddDescribes the possible measurement grades and their associated values
	 */
	public static enum MeasurementGrade {

		/**
		 *
		 */
		EXCELLENT(new BigDecimal("0.0200")),

		/**
		 *
		 */
		GOOD(new BigDecimal("0.0500")),

		/**
		 *
		 */
		FAIR(new BigDecimal("0.0800")),

		/**
		 *
		 */
		POOR(new BigDecimal("0.100"));

		private final BigDecimal percentageOfError;

		private MeasurementGrade(BigDecimal inPercentageOfError) {
			percentageOfError = inPercentageOfError;
		}

		/**
		 * Calculates the error of the associated field visit measurement
		 * 
		 * @param inMeasurementNumber The number of the actual measurement
		 * @param controlCondition The associated control condition
		 * @param dischargeValue The associated discharge value
		 * @param dischargeUnits The units associated with the discharge value
		 * @param meanGageHeightUnits The units associated with the gage height
		 * @param dateTime The date and time of the measurement
		 * @param fieldVisitIdentifier The unique identifier of the associated field visit
		 * @return
		 */
		public AqcuFieldVisitMeasurement calculateError(String inMeasurementNumber, String controlCondition, BigDecimal dischargeValue, 
				String dischargeUnits, String meanGageHeightUnits, Instant dateTime, String fieldVisitIdentifier) {
			BigDecimal errorMaxDischargeInFeet = add(dischargeValue, multiply(dischargeValue, percentageOfError));
			BigDecimal errorMinDischargeInFeet = subtract(dischargeValue, multiply(dischargeValue, percentageOfError));

			AqcuFieldVisitMeasurement ret = new AqcuFieldVisitMeasurement(inMeasurementNumber, controlCondition, dischargeValue, dischargeUnits, meanGageHeightUnits,
					errorMaxDischargeInFeet, errorMinDischargeInFeet, this, dateTime, fieldVisitIdentifier);
			return ret;
		}

		/**
		 * this is a safe operation that will catch the runtime exception if not
		 * valid
		 *
		 * @param name The name of the measurement grade
		 * @see MeasurementGradeType
		 * @return null if not valid otherwise the correct enum for the string
		 * value.
		 */
		public static MeasurementGrade valueFrom(String name) {
			MeasurementGrade ret = null;
			try {
				ret = valueOf(name);
			} catch (IllegalArgumentException iae) {
				//not a valid measurementGrade
			}
			if (ret == null) {
				ret = POOR; //default missing or other grades to poor
			}
			return ret;
		}
	}
}
