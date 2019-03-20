package com.avob.openadr.server.common.vtn.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import javax.annotation.Resource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.avob.openadr.server.common.vtn.exception.OadrVTNInitializationException;
import com.avob.openadr.server.common.vtn.models.demandresponseevent.DemandResponseEvent;
import com.avob.openadr.server.common.vtn.models.demandresponseevent.DemandResponseEventDao;
import com.avob.openadr.server.common.vtn.models.demandresponseevent.DemandResponseEventOadrProfileEnum;
import com.avob.openadr.server.common.vtn.models.demandresponseevent.DemandResponseEventOptEnum;
import com.avob.openadr.server.common.vtn.models.demandresponseevent.DemandResponseEventSignal;
import com.avob.openadr.server.common.vtn.models.demandresponseevent.DemandResponseEventSignalDao;
import com.avob.openadr.server.common.vtn.models.demandresponseevent.DemandResponseEventStateEnum;
import com.avob.openadr.server.common.vtn.models.demandresponseevent.DemandResponseEventTarget;
import com.avob.openadr.server.common.vtn.models.demandresponseevent.DemandResponseEventTargetInterface;
import com.avob.openadr.server.common.vtn.models.demandresponseevent.dto.DemandResponseEventCreateDto;
import com.avob.openadr.server.common.vtn.models.demandresponseevent.dto.DemandResponseEventUpdateDto;
import com.avob.openadr.server.common.vtn.models.demandresponseevent.dto.embedded.DemandResponseEventTargetDto;
import com.avob.openadr.server.common.vtn.models.ven.Ven;
import com.avob.openadr.server.common.vtn.models.ven.VenDao;
import com.avob.openadr.server.common.vtn.models.vendemandresponseevent.VenDemandResponseEvent;
import com.avob.openadr.server.common.vtn.models.vendemandresponseevent.VenDemandResponseEventDao;
import com.avob.openadr.server.common.vtn.service.dtomapper.DtoMapper;
import com.avob.openadr.server.common.vtn.service.push.DemandResponseEventPublisher;
import com.google.common.collect.Lists;

@Service
@Transactional
public class DemandResponseEventService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DemandResponseEventService.class);

	@Value("${oadr.supportPush:#{false}}")
	private Boolean supportPush;

	@Resource
	private VenDao venDao;

	@Resource
	private DemandResponseEventDao demandResponseEventDao;

	@Resource
	private VenDemandResponseEventDao venDemandResponseEventDao;

	@Resource
	private DemandResponseEventPublisher demandResponseEventPublisher;

	@Resource
	private DemandResponseEventSignalDao demandResponseEventSignalDao;

	@Resource
	private Executor executor;

	@Resource
	private DtoMapper dtoMapper;

	private static DatatypeFactory datatypeFactory;
	static {
		try {
			datatypeFactory = DatatypeFactory.newInstance();

		} catch (DatatypeConfigurationException e) {
			LOGGER.error("", e);
			throw new OadrVTNInitializationException(e);
		}
	}

	public Iterable<DemandResponseEvent> findAll() {
		return demandResponseEventDao.findAll();
	}

	public List<DemandResponseEvent> find(String venUsername, DemandResponseEventStateEnum state) {
		return this.find(venUsername, state, null);
	}

	public List<DemandResponseEvent> find(String venUsername, DemandResponseEventStateEnum state, Long size) {
		Pageable limit = null;
		if (size != null) {
			int i = (int) (long) size;
			limit = PageRequest.of(0, i);
		}

		Iterable<DemandResponseEvent> events = new ArrayList<DemandResponseEvent>();

		if (venUsername == null && state == null && limit == null) {
			events = demandResponseEventDao.findAll();

		} else if (venUsername == null && state == null && limit != null) {
			events = demandResponseEventDao.findAll(limit);

		} else if (venUsername != null && state == null && limit == null) {
			events = demandResponseEventDao.findByVenUsername(venUsername);

		} else if (venUsername != null && state == null && limit != null) {
			events = demandResponseEventDao.findByVenUsername(venUsername, limit);

		} else if (venUsername == null && state != null && limit == null) {
			events = demandResponseEventDao.findByDescriptorState(state);

		} else if (venUsername == null && state != null && limit != null) {
			events = demandResponseEventDao.findByDescriptorState(state, limit);

		} else if (venUsername != null && state != null && limit == null) {
			events = demandResponseEventDao.findByVenUsernameAndState(venUsername, state);

		} else if (venUsername != null && state != null && limit != null) {
			events = demandResponseEventDao.findByVenUsernameAndState(venUsername, state, limit);
		}

		return Lists.newArrayList(events);
	}

	private List<Ven> findVenForTarget(DemandResponseEvent event,
			List<? extends DemandResponseEventTargetInterface> targets) {
		List<Ven> ven = null;

		if (targets != null) {
			List<String> targetedVenUsername = new ArrayList<>();

			for (DemandResponseEventTargetInterface target : targets) {
				if ("ven".equals(target.getTargetType())) {
					targetedVenUsername.add(target.getTargetId());
				}
			}
			ven = venDao.findByUsernameInAndVenMarketContextsContains(targetedVenUsername,
					event.getDescriptor().getMarketContext());
		} else {
			ven = venDao.findByVenMarketContextsName(event.getDescriptor().getMarketContext().getName());
		}
		return ven;
	}

	/**
	 * create a DR Event
	 * 
	 * @param event
	 * @return
	 */
	@Transactional(readOnly = false)
	public DemandResponseEvent create(DemandResponseEventCreateDto dto) {
		DemandResponseEvent event = dtoMapper.map(dto, DemandResponseEvent.class);
		event.setCreatedTimestamp(System.currentTimeMillis());
		event.setLastUpdateTimestamp(System.currentTimeMillis());
		if(dto.isPublished()) {
			event.getDescriptor().setModificationNumber(0);
		}
		else {
			event.getDescriptor().setModificationNumber(-1);
		}
		

		Date dateStart = new Date();
		dateStart.setTime(event.getActivePeriod().getStart());

		// compute end from start and duration
		Duration duration = datatypeFactory.newDuration(event.getActivePeriod().getDuration());
		long durationInMillis = duration.getTimeInMillis(dateStart);
		event.getActivePeriod().setEnd(dateStart.getTime() + durationInMillis);

		// compute startNotification from start and notificationDuration
		Duration notificationDuration = datatypeFactory.newDuration(event.getActivePeriod().getNotificationDuration());
		long notificationDurationInMillis = notificationDuration.getTimeInMillis(dateStart);
		event.getActivePeriod().setStartNotification(dateStart.getTime() - notificationDurationInMillis);

		// save event
		DemandResponseEvent save = demandResponseEventDao.save(event);

		// link targets
		List<Ven> findByUsernameIn = findVenForTarget(event, dto.getTargets());
		List<VenDemandResponseEvent> list = new ArrayList<VenDemandResponseEvent>();
		for (Ven ven : findByUsernameIn) {
			if (supportPush && ven.getPushUrl() != null && demandResponseEventPublisher != null) {
				if (event.isPublished()) {
					pushAsync(Arrays.asList(ven), dto.getDescriptor().getOadrProfile());
				}
			}
			list.add(new VenDemandResponseEvent(event, ven));
		}
		venDemandResponseEventDao.saveAll(list);

		// link signals
		event.getSignals().forEach(sig -> {
			sig.setEvent(save);
		});
		demandResponseEventSignalDao.saveAll(event.getSignals());

		return save;
	}

	@Transactional(readOnly = false)
	public DemandResponseEvent update(DemandResponseEvent event, DemandResponseEventUpdateDto dto) {

		List<DemandResponseEventTargetDto> toAdd = new ArrayList<>();
		List<DemandResponseEventTargetDto> unchanged = new ArrayList<>();

		for (DemandResponseEventTargetDto updateTarget : dto.getTargets()) {
			boolean found = false;
			for (DemandResponseEventTarget target : event.getTargets()) {
				if (target.getTargetType().equals(updateTarget.getTargetType())
						&& target.getTargetId().equals(updateTarget.getTargetId())) {
					found = true;
					unchanged.add(updateTarget);
				}
			}
			if (!found) {
				toAdd.add(updateTarget);
			}

		}

		List<DemandResponseEventTarget> toRemove = new ArrayList<>();
		if (dto.getTargets().size() < unchanged.size() + toAdd.size()) {
			for (DemandResponseEventTarget target : event.getTargets()) {
				boolean found = false;
				for (DemandResponseEventTargetDto updateTarget : dto.getTargets()) {
					if (target.getTargetType().equals(updateTarget.getTargetType())
							&& target.getTargetId().equals(updateTarget.getTargetId())) {
						found = true;
					}
				}

				if (!found) {
					toRemove.add(target);
				}
			}
		}

		// update modification number if event is published
		if (dto.isPublished()) {
			event.getDescriptor().setModificationNumber(event.getDescriptor().getModificationNumber() + 1);
		}

		DemandResponseEvent partialUpdate = dtoMapper.map(dto, DemandResponseEvent.class);
		// link signals
		demandResponseEventSignalDao.deleteAll(event.getSignals());
		partialUpdate.getSignals().forEach(sig -> {
			sig.setEvent(event);
		});
		demandResponseEventSignalDao.saveAll(partialUpdate.getSignals());
		// update event
		event.setSignals(partialUpdate.getSignals());
		event.setTargets(partialUpdate.getTargets());
		event.setLastUpdateTimestamp(System.currentTimeMillis());
		DemandResponseEvent save = demandResponseEventDao.save(event);

		// link added targets
		if (toAdd.size() > 0) {
			List<Ven> vens = findVenForTarget(event, toAdd);
			List<VenDemandResponseEvent> list = new ArrayList<VenDemandResponseEvent>();
			for (Ven ven : vens) {
				if (supportPush && ven.getPushUrl() != null && demandResponseEventPublisher != null) {
					if (dto.isPublished()) {
						pushAsync(Arrays.asList(ven), event.getDescriptor().getOadrProfile());
					}
				}
				list.add(new VenDemandResponseEvent(event, ven));
			}
			venDemandResponseEventDao.saveAll(list);
		}

		// unlink removed target
		if (toRemove.size() > 0) {
			List<Ven> vens = findVenForTarget(event, toRemove);
			venDemandResponseEventDao.deleteByEventIdAndVenIn(event.getId(), vens);
		}

		return save;
	}

	public void distributeEventToPushVen(DemandResponseEvent event) {
		List<Ven> vens = findVenForTarget(event, event.getTargets());
		for (Ven ven : vens) {
			if (supportPush && ven.getPushUrl() != null && demandResponseEventPublisher != null) {
				pushAsync(Arrays.asList(ven), event.getDescriptor().getOadrProfile());
			}
		}
	}

	@Transactional(readOnly = false)
	public DemandResponseEvent publish(DemandResponseEvent event) {
		if (!event.isPublished()) {
			event.getDescriptor().setModificationNumber(event.getDescriptor().getModificationNumber() + 1);
			event.setPublished(true);
			demandResponseEventDao.save(event);
		}

		distributeEventToPushVen(event);
		return event;
	}

	@Transactional(readOnly = false)
	public DemandResponseEvent active(DemandResponseEvent event) {
		if (!event.getDescriptor().getState().equals(DemandResponseEventStateEnum.ACTIVE)) {
			event.getDescriptor().setState(DemandResponseEventStateEnum.ACTIVE);
			event.getDescriptor().setModificationNumber(event.getDescriptor().getModificationNumber() + 1);
			event.setPublished(true);
			distributeEventToPushVen(event);
			demandResponseEventDao.save(event);

		}
		return event;
	}

	@Transactional(readOnly = false)
	public DemandResponseEvent cancel(DemandResponseEvent event) {
		if (!event.getDescriptor().getState().equals(DemandResponseEventStateEnum.CANCELED)) {
			event.getDescriptor().setState(DemandResponseEventStateEnum.CANCELED);
			event.getDescriptor().setModificationNumber(event.getDescriptor().getModificationNumber() + 1);
			event.setPublished(true);
			distributeEventToPushVen(event);
			demandResponseEventDao.save(event);
		}
		return event;
	}

	private void pushAsync(List<Ven> vens, DemandResponseEventOadrProfileEnum profile) {
		Runnable run = new Runnable() {

			@Override
			public void run() {
				for (Ven ven : vens) {
					// the receiver check if ven is a push ven and how it
					// does
					// make
					// event available if not
					if (DemandResponseEventOadrProfileEnum.OADR20B.equals(profile)
							&& DemandResponseEventOadrProfileEnum.OADR20B.getCode().equals(ven.getOadrProfil())) {
						demandResponseEventPublisher.publish20b(ven);
					} else if (DemandResponseEventOadrProfileEnum.OADR20A.equals(profile)) {
						demandResponseEventPublisher.publish20a(ven);
					}
				}
			}

		};

		executor.execute(run);

	}

	public Optional<DemandResponseEvent> findById(Long id) {
		return demandResponseEventDao.findById(id);
	}

	public DemandResponseEvent findByEventId(String eventId) {
		return demandResponseEventDao.findOneByDescriptorEventId(eventId);
	}

	public List<DemandResponseEventSignal> getSignals(DemandResponseEvent event) {
		return demandResponseEventSignalDao.findByEvent(event);
	}

	public boolean delete(Long id) {
		boolean exists = demandResponseEventDao.existsById(id);
		if (exists) {
			venDemandResponseEventDao.deleteByEventId(id);
			demandResponseEventDao.deleteById(id);
		}
		return exists;
	}

	public void delete(Iterable<DemandResponseEvent> entities) {
		venDemandResponseEventDao.deleteByEventIn(Lists.newArrayList(entities));
		demandResponseEventDao.deleteAll(entities);
	}

	public void deleteById(Iterable<Long> entities) {
		for (Long id : entities) {
			venDemandResponseEventDao.deleteByEventId(id);

		}

		demandResponseEventDao.deleteByIdIn(entities);
	}

	public boolean exists(Long id) {
		return demandResponseEventDao.existsById(id);
	}

	public List<DemandResponseEvent> findToSentEventByVen(Ven ven) {
		return this.findToSentEventByVen(ven, null);
	}

	public List<DemandResponseEvent> findToSentEventByVen(Ven ven, Long size) {
		long currentTimeMillis = System.currentTimeMillis();

		if (size != null && size > 0) {
			Pageable limit = null;
			int i = (int) (long) size;
			limit = PageRequest.of(0, i);
			return demandResponseEventDao.findToSentEventByVen(ven, currentTimeMillis, limit);
		} else {
			return demandResponseEventDao.findToSentEventByVen(ven, currentTimeMillis);
		}
	}

	public boolean hasResponded(String venId, DemandResponseEvent event) {
		VenDemandResponseEvent findOneByEventAndVen = venDemandResponseEventDao.findOneByEventAndVenId(event, venId);
		return findOneByEventAndVen.getVenOpt() != null;
	}

	/**
	 * update ven optin for a specific event
	 * 
	 * @param demandResponseEvent
	 * @param ven
	 * @param venOpt
	 */
	public void updateVenOpt(Long demandResponseEventId, Long modificationNumber, String venUsername,
			DemandResponseEventOptEnum venOpt) {

		VenDemandResponseEvent findOneByEventIdAndVenId = venDemandResponseEventDao
				.findOneByEventIdAndVenUsername(demandResponseEventId, venUsername);

		if (findOneByEventIdAndVenId != null) {
			findOneByEventIdAndVenId.setVenOpt(venOpt);
			findOneByEventIdAndVenId.setLastSentModificationNumber(modificationNumber);
			venDemandResponseEventDao.save(findOneByEventIdAndVenId);
		}
	}

	/**
	 * get ven optin for a specific event
	 * 
	 * @param demandResponseEvent
	 * @param ven
	 * @param venOpt
	 */
	public DemandResponseEventOptEnum getVenOpt(Long demandResponseEventId, String venUsername) {

		VenDemandResponseEvent findOneByEventIdAndVenId = venDemandResponseEventDao
				.findOneByEventIdAndVenUsername(demandResponseEventId, venUsername);

		if (findOneByEventIdAndVenId != null) {
			return findOneByEventIdAndVenId.getVenOpt();
		}
		return null;
	}

	public List<DemandResponseEvent> findToSentEventByVenUsername(String username) {
		return this.findToSentEventByVenUsername(username, null);
	}

	public List<DemandResponseEvent> findToSentEventByVenUsername(String username, Long size) {
		long currentTimeMillis = System.currentTimeMillis() - 60 * 1000;
		if (size != null && size > 0) {
			Pageable limit = null;
			int i = (int) (long) size;
			limit = PageRequest.of(0, i);
			return demandResponseEventDao.findToSentEventByVenUsername(username, currentTimeMillis, limit);
		} else {
			return demandResponseEventDao.findToSentEventByVenUsername(username, currentTimeMillis);
		}
	}

}
