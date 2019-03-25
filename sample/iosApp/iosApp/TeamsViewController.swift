//
//  TeamsViewController.swift
//  iosApp
//
//  Copyright © 2019 Square, Inc.. All rights reserved.
//

import Foundation
import UIKit
import main

class TeamsViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {
    var teamData:[Team] = TeamData().teams()
    
    @IBOutlet weak var tableView: UITableView!
    override func viewDidLoad() {
        super.viewDidLoad()
        
        tableView.delegate = self
        tableView.dataSource = self
    }
    
    func showTeams(teamData:TeamData) -> KotlinUnit{
        tableView.reloadData()
        return KotlinUnit()
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: false)
        
        let team = teamData[indexPath.row]
        
        performSegue(withIdentifier: "ShowPlayers", sender: team)
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "ShowPlayers" {
            let detailViewController = segue.destination as! PlayersViewController
            let team = sender as! Team
            detailViewController.teamId = team.id
        }
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return teamData.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "teamRow", for: indexPath) as! TeamRow
        let team = teamData[indexPath.row]
        cell.fillName(name: team.name)
        cell.fillCoach(coach: team.coach)
        cell.fillFounded(founded: DateFormatHelperKt.defaultFormatter.format(d: team.founded))
        
        cell.layer.isOpaque = true
        
        return cell
    }
}
