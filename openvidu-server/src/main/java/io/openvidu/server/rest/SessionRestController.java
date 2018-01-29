/*
 * (C) Copyright 2017 OpenVidu (http://openvidu.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.openvidu.server.rest;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.openvidu.client.OpenViduException;
import io.openvidu.java.client.ArchiveLayout;
import io.openvidu.java.client.ArchiveMode;
import io.openvidu.java.client.MediaMode;
import io.openvidu.java.client.SessionProperties;
import io.openvidu.server.core.ParticipantRole;
import io.openvidu.server.core.SessionManager;

/**
 *
 * @author Pablo Fuente Pérez
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class SessionRestController {

	private static final int UPDATE_SPEAKER_INTERVAL_DEFAULT = 1800;
	private static final int THRESHOLD_SPEAKER_DEFAULT = -50;

	@Autowired
	private SessionManager sessionManager;

	@RequestMapping(value = "/sessions", method = RequestMethod.GET)
	public Set<String> getAllSessions() {
		return sessionManager.getSessions();
	}

	@RequestMapping("/getUpdateSpeakerInterval")
	public Integer getUpdateSpeakerInterval() {
		return Integer.valueOf(getProperty("updateSpeakerInterval", UPDATE_SPEAKER_INTERVAL_DEFAULT));
	}

	@RequestMapping("/getThresholdSpeaker")
	public Integer getThresholdSpeaker() {
		return Integer.valueOf(getProperty("thresholdSpeaker", THRESHOLD_SPEAKER_DEFAULT));
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/sessions", method = RequestMethod.POST)
	public ResponseEntity<JSONObject> getSessionId(@RequestBody(required = false) Map<?, ?> params) {
		
		SessionProperties.Builder builder = new SessionProperties.Builder();
		if (params != null) {
			String archiveModeString = (String) params.get("archiveMode");
			String archiveLayoutString = (String) params.get("archiveLayout");
			String mediaModeString = (String) params.get("mediaMode");

			try {
				if (archiveModeString != null) {
					ArchiveMode archiveMode = ArchiveMode.valueOf(archiveModeString);
					builder = builder.archiveMode(archiveMode);
				}
				if (archiveLayoutString != null) {
					ArchiveLayout archiveLayout = ArchiveLayout.valueOf(archiveLayoutString);
					builder = builder.archiveLayout(archiveLayout);
				}
				if (mediaModeString != null) {
					MediaMode mediaMode = MediaMode.valueOf(mediaModeString);
					builder = builder.mediaMode(mediaMode);
				}
			} catch (IllegalArgumentException e) {
				return this.generateErrorResponse("ArchiveMode " + params.get("archiveMode") + " | " + "ArchiveLayout "
						+ params.get("archiveLayout") + " | " + "MediaMode " + params.get("mediaMode")
						+ " are not defined", "/api/tokens");
			}
		}

		SessionProperties sessionProperties = builder.build();

		String sessionId = sessionManager.newSessionId(sessionProperties);
		JSONObject responseJson = new JSONObject();
		responseJson.put("id", sessionId);
		return new ResponseEntity<JSONObject>(responseJson, HttpStatus.OK);
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/tokens", method = RequestMethod.POST)
	public ResponseEntity<JSONObject> newToken(@RequestBody Map<?, ?> params) {
		try {

			String sessionId = (String) params.get("session");
			ParticipantRole role = ParticipantRole.valueOf((String) params.get("role"));
			String metadata = (String) params.get("data");

			String token = sessionManager.newToken(sessionId, role, metadata);
			JSONObject responseJson = new JSONObject();
			responseJson.put("id", token);
			responseJson.put("session", sessionId);
			responseJson.put("role", role.toString());
			responseJson.put("data", metadata);
			responseJson.put("token", token);
			return new ResponseEntity<JSONObject>(responseJson, HttpStatus.OK);

		} catch (IllegalArgumentException e) {
			return this.generateErrorResponse("Role " + params.get("role") + " is not defined", "/api/tokens");
		} catch (OpenViduException e) {
			return this.generateErrorResponse(
					"Metadata [" + params.get("data") + "] unexpected format. Max length allowed is 1000 chars",
					"/api/tokens");
		}
	}

	@SuppressWarnings("unchecked")
	private ResponseEntity<JSONObject> generateErrorResponse(String errorMessage, String path) {
		JSONObject responseJson = new JSONObject();
		responseJson.put("timestamp", System.currentTimeMillis());
		responseJson.put("status", HttpStatus.BAD_REQUEST.value());
		responseJson.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
		responseJson.put("message", errorMessage);
		responseJson.put("path", path);
		return new ResponseEntity<JSONObject>(responseJson, HttpStatus.BAD_REQUEST);
	}
}
