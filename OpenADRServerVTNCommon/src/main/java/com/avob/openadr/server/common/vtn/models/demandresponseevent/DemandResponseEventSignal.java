package com.avob.openadr.server.common.vtn.models.demandresponseevent;

import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.avob.openadr.server.common.vtn.models.ItemBase;
import com.avob.openadr.server.common.vtn.models.Target;

@Entity
@Table(name = "demandresponseeventsignal")
public class DemandResponseEventSignal {
	/**
	 * Autogenerated unique id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@NotNull
	private DemandResponseEventSignalNameEnum signalName;

	@NotNull
	private DemandResponseEventSignalTypeEnum signalType;

	@ElementCollection(fetch = FetchType.EAGER)
	private List<DemandResponseEventSignalInterval> intervals;

	private Float currentValue;

	private ItemBase itemBase;

	@ElementCollection
	@LazyCollection(LazyCollectionOption.FALSE)
	private List<Target> targets;

	/**
	 * Related event
	 */
	@ManyToOne
	@JoinColumn(name = "demandresponseevent_id")
	private DemandResponseEvent event;

	public DemandResponseEventSignalNameEnum getSignalName() {
		return signalName;
	}

	public void setSignalName(DemandResponseEventSignalNameEnum signalName) {
		this.signalName = signalName;
	}

	public DemandResponseEventSignalTypeEnum getSignalType() {
		return signalType;
	}

	public void setSignalType(DemandResponseEventSignalTypeEnum signalType) {
		this.signalType = signalType;
	}

	public List<DemandResponseEventSignalInterval> getIntervals() {
		return intervals;
	}

	public void setIntervals(List<DemandResponseEventSignalInterval> intervals) {
		this.intervals = intervals;
	}

	public Float getCurrentValue() {
		return currentValue;
	}

	public void setCurrentValue(Float currentValue) {
		this.currentValue = currentValue;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public DemandResponseEvent getEvent() {
		return event;
	}

	public void setEvent(DemandResponseEvent event) {
		this.event = event;
	}

	public ItemBase getItemBase() {
		return itemBase;
	}

	public void setItemBase(ItemBase itemBase) {
		this.itemBase = itemBase;
	}

	public List<Target> getTargets() {
		return targets;
	}

	public void setTargets(List<Target> targets) {
		this.targets = targets;
	}

}
